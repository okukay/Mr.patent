from fastapi import APIRouter, HTTPException, BackgroundTasks
from typing import Dict, Any, List
from datetime import datetime, timezone
import numpy as np
from sklearn.metrics.pairwise import cosine_similarity
import json
import re
import traceback

from app.core.database import database
from app.services.vectorizer import get_tfidf_vector, get_bert_vector
from app.services.kipris import get_patent_public_info, download_patent_pdf

router = APIRouter(prefix="/fastapi", tags=["similarity"])

def get_current_timestamp():
    """현재 시간을 ISO 8601 형식으로 변환 (UTC)"""
    return datetime.now(timezone.utc).isoformat().replace('+00:00', 'Z')

# 벡터 차원 불일치 문제와 NaN 문제를 해결하는 안전한 변환 함수
def safe_frombuffer(buffer, target_dim=1000, dtype=np.float32):
    """안전하게 버퍼를 배열로 변환하고 차원을 맞춰주는 함수"""
    if buffer is None or len(buffer) == 0:
        return np.zeros(target_dim, dtype=dtype)
    
    try:
        # 버퍼를 배열로 변환
        vec = np.frombuffer(buffer, dtype=dtype)
        
        # NaN 값 처리 - 추가된 부분
        if np.isnan(vec).any():
            vec = np.nan_to_num(vec, nan=0.0)
        
        # 차원 불일치 해결
        if len(vec) > target_dim:
            # 더 큰 경우 잘라내기
            return vec[:target_dim]
        elif len(vec) < target_dim:
            # 더 작은 경우 제로 패딩
            result = np.zeros(target_dim, dtype=dtype)
            result[:len(vec)] = vec
            return result
        return vec
    except ValueError:
        # 버퍼 크기 오류 등 예외 발생 시 영벡터 반환
        return np.zeros(target_dim, dtype=dtype)

# 안전한 코사인 유사도 계산 함수
def safe_cosine_similarity(a, b):
    """NaN을 처리하는 안전한 코사인 유사도 계산 함수"""
    # NaN 값 확인 및 처리
    a = np.nan_to_num(a, nan=0.0)
    b = np.nan_to_num(b, nan=0.0)
    
    # 벡터가 모두 0인 경우 처리
    if np.linalg.norm(a) == 0 or np.linalg.norm(b) == 0:
        return 0.0
    
    try:
        return float(cosine_similarity([a], [b])[0][0])
    except Exception:
        # 기타 오류 발생 시 0 반환
        return 0.0

@router.post("/draft/{patent_draft_id}/similarity-check", response_model=Dict[str, Any])
async def run_similarity_check(patent_draft_id: int, background_tasks: BackgroundTasks):
    """특허 초안의 적합도 검사, 유사도 분석, 상세 비교를 모두 수행 (비동기)"""
    # 1. 특허 초안 존재 확인
    draft_query = """
    SELECT * FROM patent_draft 
    WHERE patent_draft_id = :draft_id
    """
    draft = await database.fetch_one(
        query=draft_query,
        values={"draft_id": patent_draft_id}
    )
    
    if not draft:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "DRAFT_NOT_FOUND",
                "message": "해당 ID의 특허 초안을 찾을 수 없습니다."
            }
        )
    
    # 2. similarity 엔티티 생성 (분석 상태 표시)
    now = datetime.now(timezone.utc)
    similarity_query = """
    INSERT INTO similarity (
        patent_draft_id, 
        similarity_created_at, 
        similarity_updated_at
    ) VALUES (
        :draft_id,
        :created_at,
        :updated_at
    )
    """
    
    similarity_id = await database.execute(
        query=similarity_query,
        values={
            "draft_id": patent_draft_id,
            "created_at": now,
            "updated_at": now
        }
    )
    
    # 3. 백그라운드에서 분석 작업 실행
    background_tasks.add_task(
        perform_similarity_analysis,  # 실행할 함수
        patent_draft_id,              # 첫 번째 인자
        similarity_id                 # 두 번째 인자
    )
    
    # 4. 즉시 응답 반환 (작업 시작됨)
    return {
        "data": {
            "similarity_id": similarity_id,
            "status": "ANALYZING"
        }
    }

# 새로 추가: 적합도 결과 조회 API
@router.get("/patent/fitness/{patent_draft_id}", response_model=Dict[str, Any])
async def get_fitness_result(patent_draft_id: int):
    """특허 초안의 적합도 검사 결과 조회"""
    # 데이터베이스에서 적합도 결과 조회
    query = """
    SELECT * FROM fitness
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY fitness_created_at DESC
    LIMIT 1
    """
    
    fitness = await database.fetch_one(
        query=query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    if not fitness:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "FITNESS_NOT_FOUND",
                "message": "해당 특허 초안의 적합도 검사 결과가 없습니다."
            }
        )
    
    # Record 객체를 딕셔너리로 변환
    fitness_dict = dict(fitness)
    
    # fitness_good_content가 JSON 문자열인 경우 파싱
    fitness_good_content = fitness_dict.get("fitness_good_content")
    if fitness_good_content and isinstance(fitness_good_content, str):
        try:
            fitness_good_content = json.loads(fitness_good_content)
        except json.JSONDecodeError:
            fitness_good_content = {}
    
    return {
        "data": {
            "fitness_id": fitness_dict["fitness_id"],
            "patent_draft_id": fitness_dict["patent_draft_id"],
            "is_corrected": fitness_dict["fitness_is_corrected"],
            "details": fitness_good_content,
            "created_at": fitness_dict["fitness_created_at"]
        }
    }

# 새로 추가: 유사도 결과 조회 API
@router.get("/patent/similarity/{patent_draft_id}", response_model=Dict[str, Any])
async def get_similarity_result(patent_draft_id: int):
    """특허 초안의 유사도 검사 결과 조회"""
    # 특허 초안 정보 조회
    draft_query = """
    SELECT * FROM patent_draft 
    WHERE patent_draft_id = :draft_id
    """
    
    draft = await database.fetch_one(
        query=draft_query,
        values={"draft_id": patent_draft_id}
    )
    
    if not draft:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "DRAFT_NOT_FOUND",
                "message": "해당 ID의 특허 초안을 찾을 수 없습니다."
            }
        )
    
    # 유사도 분석 결과 조회
    similarity_query = """
    SELECT * FROM similarity
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY similarity_created_at DESC
    LIMIT 1
    """
    
    similarity = await database.fetch_one(
        query=similarity_query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    if not similarity:
        raise HTTPException(
            status_code=404,
            detail={
                "code": "SIMILARITY_NOT_FOUND",
                "message": "해당 특허 초안의 유사도 분석 결과가 없습니다."
            }
        )
    
    # Record 객체를 딕셔너리로 변환
    draft_dict = dict(draft)
    similarity_dict = dict(similarity)
    
    # 유사 특허 목록 조회
    similar_patents_query = """
    SELECT sp.*, p.patent_title, p.patent_application_number
    FROM similarity_patent sp
    JOIN patent p ON sp.patent_id = p.patent_id
    WHERE sp.similarity_id = :similarity_id
    ORDER BY sp.similarity_patent_score DESC
    """
    
    similar_patents = await database.fetch_all(
        query=similar_patents_query,
        values={"similarity_id": similarity_dict["similarity_id"]}
    )
    
    # 상세 비교 결과 조회
    comparisons_query = """
    SELECT * FROM detailed_comparison
    WHERE patent_draft_id = :patent_draft_id
    ORDER BY detailed_comparison_total_score DESC
    """
    
    comparisons = await database.fetch_all(
        query=comparisons_query,
        values={"patent_draft_id": patent_draft_id}
    )
    
    # 결과 포맷팅
    similar_patents_data = []
    for patent in similar_patents:
        # Record 객체를 딕셔너리로 변환
        patent_dict = dict(patent)
        similar_patents_data.append({
            "patent_id": patent_dict["patent_id"],
            "patent_title": patent_dict["patent_title"],
            "patent_application_number": patent_dict["patent_application_number"],
            "similarity_score": patent_dict["similarity_patent_score"],
            "title_score": patent_dict["similarity_patent_title"],
            "summary_score": patent_dict["similarity_patent_summary"],
            "claim_score": patent_dict["similarity_patent_claim"]
        })
    
    comparisons_data = []
    for comp in comparisons:
        # Record 객체를 딕셔너리로 변환
        comp_dict = dict(comp)
        context_data = comp_dict["detailed_comparison_context"]
        # 컨텍스트 데이터가 JSON 문자열인 경우 파싱
        if context_data and isinstance(context_data, str):
            try:
                context_data = json.loads(context_data)
            except json.JSONDecodeError:
                context_data = {}
        
        comparisons_data.append({
            "patent_id": comp_dict["patent_id"],
            "total_score": comp_dict["detailed_comparison_total_score"],
            "context": context_data
        })
    
    return {
        "data": {
            "similarity_id": similarity_dict["similarity_id"],
            "patent_draft_id": patent_draft_id,
            "similar_patents": similar_patents_data,
            "comparisons": comparisons_data,
            "created_at": similarity_dict["similarity_created_at"]
        }
    }

async def perform_fitness_check(patent_draft_id: int, draft, now: datetime) -> Dict:
    """적합도 검사 수행"""
    print(f"적합도 검사 실행: 특허 초안 ID {patent_draft_id}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    # 검사 결과 저장용 딕셔너리
    fitness_results = {
        "is_corrected": True,
        "details": {}
    }

    # 각 섹션별 적합도 검사
    sections = [
        ("technical_field", "기술 분야"),
        ("background", "배경 기술"),
        ("problem", "해결 과제"),
        ("solution", "해결 방법"),
        ("effect", "효과"),
        ("summary", "요약"),
        ("claim", "청구항")
    ]

    for section_key, section_name in sections:
        # 딕셔너리 메소드 사용
        field_key = f"patent_draft_{section_key}"
        section_text = draft_dict.get(field_key, "")
        
        # 1. 내용 존재 확인
        if not section_text or len(section_text.strip()) < 10:
            fitness_results["is_corrected"] = False
            fitness_results["details"][section_key] = False
            continue

        # 2. 개선된 문맥 적합도 검사 사용
        try:
            similarity_score = improved_context_fitness(draft_dict, section_key, section_text)
            # 적합도 기준 점수를 0.4로 낮춤 (더 관대하게)
            if similarity_score < 0.4:
                fitness_results["is_corrected"] = False
                fitness_results["details"][section_key] = False
            else:
                fitness_results["details"][section_key] = True
        except Exception as e:
            print(f"{section_name} 문맥 적합도 검사 중 오류: {str(e)}")
            fitness_results["is_corrected"] = False
            fitness_results["details"][section_key] = False
    
    # 데이터베이스에 적합도 결과 저장
    try:
        fitness_query = """
        INSERT INTO fitness (
            patent_draft_id,
            fitness_is_corrected,
            fitness_good_content,
            fitness_created_at,
            fitness_updated_at
        ) VALUES (
            :patent_draft_id,
            :is_corrected,
            :good_content,
            :created_at,
            :updated_at
        )
        """
        
        await database.execute(
            query=fitness_query,
            values={
                "patent_draft_id": patent_draft_id,
                "is_corrected": fitness_results["is_corrected"],
                "good_content": json.dumps(fitness_results["details"]),
                "created_at": now,
                "updated_at": now
            }
        )
        print(f"적합도 검사 결과 저장 완료: {fitness_results['is_corrected']}")
    except Exception as e:
        print(f"적합도 결과 저장 중 오류: {str(e)}")
    
    return fitness_results

async def perform_tfidf_similarity(patent_draft_id: int, draft) -> List[Dict]:
    """TF-IDF 기반 1차 유사도 검사"""
    print(f"TF-IDF 유사도 분석 시작: 특허 초안 ID {patent_draft_id}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    # 초안에서 TF-IDF 벡터 추출 - NaN 처리 및 차원 표준화
    draft_title_tfidf = safe_frombuffer(draft_dict["patent_draft_title_tfidf_vector"], target_dim=1000)
    draft_summary_tfidf = safe_frombuffer(draft_dict["patent_draft_summary_tfidf_vector"], target_dim=1000)
    draft_claim_tfidf = safe_frombuffer(draft_dict["patent_draft_claim_tfidf_vector"], target_dim=1000)
    
    # TF-IDF 기반 초기 유사도 계산 (모든 특허에 대해)
    limit = 1000
    offset = 0
    all_tfidf_candidates = []
    
    while True:
        # 특허 배치 조회 (TF-IDF 벡터만 가져옴 - 메모리 절약)
        patents_query = """
        SELECT patent_id, patent_title, patent_summary, patent_claim,
               patent_application_number, 
               patent_title_tfidf_vector,
               patent_summary_tfidf_vector,
               patent_claim_tfidf_vector
        FROM patent
        LIMIT :limit OFFSET :offset
        """
        
        patents = await database.fetch_all(
            query=patents_query,
            values={"limit": limit, "offset": offset}
        )
        
        if not patents:
            break
            
        # 각 특허와 TF-IDF 유사도 계산
        for patent in patents:
            try:
                # TF-IDF 벡터 추출 - NaN 처리 및 차원 표준화
                patent_title_tfidf = safe_frombuffer(patent["patent_title_tfidf_vector"], target_dim=1000)
                patent_summary_tfidf = safe_frombuffer(patent["patent_summary_tfidf_vector"], target_dim=1000)
                patent_claim_tfidf = safe_frombuffer(patent["patent_claim_tfidf_vector"], target_dim=1000)
                
                # 안전한 코사인 유사도 계산 함수 사용
                title_tfidf_similarity = safe_cosine_similarity(draft_title_tfidf, patent_title_tfidf)
                summary_tfidf_similarity = safe_cosine_similarity(draft_summary_tfidf, patent_summary_tfidf)
                claim_tfidf_similarity = safe_cosine_similarity(draft_claim_tfidf, patent_claim_tfidf)
                
                # 필드별 가중치 적용한 전체 유사도
                overall_tfidf_similarity = (0.3 * title_tfidf_similarity + 0.3 * summary_tfidf_similarity + 0.4 * claim_tfidf_similarity)
                
                all_tfidf_candidates.append({
                    "patent_id": patent["patent_id"],
                    "patent_title": patent["patent_title"],
                    "patent_summary": patent["patent_summary"],
                    "patent_claim": patent["patent_claim"],
                    "patent_application_number": patent["patent_application_number"],
                    "tfidf_similarity": overall_tfidf_similarity
                })
            except Exception as e:
                print(f"특허 {patent['patent_id']} TF-IDF 유사도 계산 중 오류: {str(e)}")
                continue
        
        offset += limit
        print(f"TF-IDF 유사도 계산 진행: {len(all_tfidf_candidates)}개 특허 처리됨")
    
    # TF-IDF 유사도 기준 정렬
    all_tfidf_candidates.sort(key=lambda x: x["tfidf_similarity"], reverse=True)
    print(f"TF-IDF 유사도 분석 완료: 총 {len(all_tfidf_candidates)}개 특허 처리")
    
    return all_tfidf_candidates

async def perform_bert_similarity(patent_draft_id: int, draft, candidates: List[Dict]) -> List[Dict]:
    """BERT 기반 2차 유사도 검사"""
    print(f"BERT 유사도 분석 시작: 특허 초안 ID {patent_draft_id}, 후보 특허 수: {len(candidates)}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    final_results = []
    
    for i, candidate in enumerate(candidates):
        print(f"BERT 유사도 계산 진행: {i+1}/{len(candidates)}")
        # 실시간 BERT 벡터 생성 및 유사도 계산
        # 텍스트 길이 제한 및 전처리
        draft_title = draft_dict["patent_draft_title"][:500]
        draft_summary = draft_dict["patent_draft_summary"][:500]
        draft_claim = draft_dict["patent_draft_claim"][:500]
        
        patent_title = candidate["patent_title"][:500] if candidate["patent_title"] else ""
        patent_summary = candidate["patent_summary"][:500] if candidate["patent_summary"] else ""
        patent_claim = candidate["patent_claim"][:500] if candidate["patent_claim"] else ""
        
        # BERT 벡터 생성
        draft_title_bert = get_bert_vector(draft_title)
        draft_summary_bert = get_bert_vector(draft_summary)
        draft_claim_bert = get_bert_vector(draft_claim)
        
        patent_title_bert = get_bert_vector(patent_title)
        patent_summary_bert = get_bert_vector(patent_summary)
        patent_claim_bert = get_bert_vector(patent_claim)
        
        # 안전한 BERT 유사도 계산
        title_bert_similarity = safe_cosine_similarity(draft_title_bert, patent_title_bert)
        summary_bert_similarity = safe_cosine_similarity(draft_summary_bert, patent_summary_bert)
        claim_bert_similarity = safe_cosine_similarity(draft_claim_bert, patent_claim_bert)
        
        # 최종 유사도 계산 (TF-IDF 30%, BERT 70%)
        title_similarity = 0.3 * candidate["tfidf_similarity"] + 0.7 * title_bert_similarity
        summary_similarity = 0.3 * candidate["tfidf_similarity"] + 0.7 * summary_bert_similarity
        claim_similarity = 0.3 * candidate["tfidf_similarity"] + 0.7 * claim_bert_similarity
        
        # 필드별 가중치 적용한 전체 유사도
        overall_similarity = (0.3 * title_similarity + 0.3 * summary_similarity + 0.4 * claim_similarity)
        
        final_results.append({
            "patent_id": candidate["patent_id"],
            "patent_application_number": candidate["patent_application_number"],
            "patent_title": candidate["patent_title"],
            "title_similarity": title_similarity,
            "summary_similarity": summary_similarity,
            "claim_similarity": claim_similarity,
            "overall_similarity": overall_similarity
        })
    
    # 최종 유사도 기준 정렬
    final_results.sort(key=lambda x: x["overall_similarity"], reverse=True)
    print(f"BERT 유사도 분석 완료: {len(final_results)}개 특허 선별")
    
    return final_results

async def save_similarity_results(patent_draft_id: int, similarity_id: int, results: List[Dict], now: datetime):
    """유사도 결과 저장"""
    print(f"유사도 결과 저장 시작: {len(results)}개 특허")
    
    for patent in results:
        try:
            similarity_patent_query = """
            INSERT INTO similarity_patent (
                patent_id,
                similarity_id,
                similarity_patent_score,
                similarity_patent_claim,
                similarity_patent_summary,
                similarity_patent_title,
                similarity_patent_created_at,
                similarity_patent_updated_at
            ) VALUES (
                :patent_id,
                :similarity_id,
                :overall_score,
                :claim_score,
                :summary_score,
                :title_score,
                :created_at,
                :updated_at
            )
            """
            
            similarity_patent_id = await database.execute(
                query=similarity_patent_query,
                values={
                    "patent_id": patent["patent_id"],
                    "similarity_id": similarity_id,
                    "overall_score": patent["overall_similarity"],
                    "claim_score": patent["claim_similarity"],
                    "summary_score": patent["summary_similarity"],
                    "title_score": patent["title_similarity"],
                    "created_at": now,
                    "updated_at": now
                }
            )
            
            # ID 추가
            patent["similarity_patent_id"] = similarity_patent_id
            
        except Exception as e:
            print(f"특허 {patent['patent_id']} 유사도 결과 저장 중 오류: {str(e)}")
    
    print(f"유사도 결과 저장 완료")
    return

async def perform_detailed_comparison(patent_draft_id: int, draft, top_results: List[Dict], now: datetime):
    """상위 특허에 대한 상세 비교 및 KIPRIS API 연동"""
    print(f"상세 비교 시작: 특허 초안 ID {patent_draft_id}, 상위 특허 수: {len(top_results)}")
    
    # 레코드 객체를 딕셔너리로 변환
    draft_dict = dict(draft) if draft else {}
    
    for i, patent in enumerate(top_results):
        try:
            print(f"상세 비교 진행 중: {i+1}/{len(top_results)}")
            patent_app_number = patent["patent_application_number"]
            
            # 1. KIPRIS API를 통해 특허 공고전문 정보 가져오기
            print(f"KIPRIS API 호출 시작: {patent_app_number}")
            patent_info = await get_patent_public_info(patent_app_number)
            
            if not patent_info:
                print(f"특허 {patent_app_number}의 KIPRIS 정보를 찾을 수 없습니다.")
                continue
            
            print(f"KIPRIS API 호출 성공: {patent_app_number}, 공고번호: {patent_info.get('publication_number', '알 수 없음')}")
            
            # 2. 특허 공고전문 데이터베이스에 저장 (변경된 테이블 구조 반영)
            patent_public_query = """
            INSERT INTO patent_public (
                patent_id,
                patent_public_number,
                patent_public_content,
                patent_public_api_response,
                patent_public_created_at,
                patent_public_updated_at
            ) VALUES (
                :patent_id,
                :public_number,
                :content,
                :api_response,
                :created_at,
                :updated_at
            ) ON DUPLICATE KEY UPDATE
                patent_public_content = :content,
                patent_public_api_response = :api_response,
                patent_public_updated_at = :updated_at
            """
            
            # 간단한 내용 예시 (실제로는 PDF 텍스트 추출 필요)
            content = f"특허 공고전문 내용 (출원번호: {patent_app_number})"
            
            print(f"특허 공고전문 DB 저장 시작: {patent_app_number}")
            patent_public_id = await database.execute(
                query=patent_public_query,
                values={
                    "patent_id": patent["patent_id"],
                    "public_number": patent_info["publication_number"],
                    "content": content,
                    "api_response": json.dumps(patent_info, ensure_ascii=False),
                    "created_at": now,
                    "updated_at": now
                }
            )
            print(f"특허 공고전문 DB 저장 완료: ID {patent_public_id}")
            
            # 3. 상세 비교 결과 저장
            # 유사 구간 하이라이트 (샘플 데이터)
            highlights = [
                {
                    "original": draft_dict["patent_draft_title"][:100],
                    "similar": patent["patent_title"][:100],
                    "similarity": patent["title_similarity"]
                },
                {
                    "original": draft_dict["patent_draft_summary"][:100],
                    "similar": patent["patent_summary"][:100] if patent.get("patent_summary") else "",
                    "similarity": patent["summary_similarity"]
                },
                {
                    "original": draft_dict["patent_draft_claim"][:100],
                    "similar": patent["patent_claim"][:100] if patent.get("patent_claim") else "",
                    "similarity": patent["claim_similarity"]
                }
            ]
            
            # 상세 비교 컨텍스트 생성
            context = {
                "highlights": highlights,
                "overall_similarity": patent["overall_similarity"]
            }
            
            # 상세 비교 결과 저장
            detailed_comparison_query = """
            INSERT INTO detailed_comparison (
                patent_draft_id,
                patent_id,
                similarity_patent_id,
                patent_public_id,
                detailed_comparison_total_score,
                detailed_comparison_context,
                detailed_comparison_created_at,
                detailed_comparison_updated_at
            ) VALUES (
                :draft_id,
                :patent_id,
                :similarity_patent_id,
                :public_id,
                :total_score,
                :context,
                :created_at,
                :updated_at
            )
            """
            
            print(f"상세 비교 결과 저장 시작: {patent_app_number}")
            await database.execute(
                query=detailed_comparison_query,
                values={
                    "draft_id": patent_draft_id,
                    "patent_id": patent["patent_id"],
                    "similarity_patent_id": patent["similarity_patent_id"],
                    "public_id": patent_public_id,
                    "total_score": patent["overall_similarity"],
                    "context": json.dumps(context, ensure_ascii=False),
                    "created_at": now,
                    "updated_at": now
                }
            )
            
            print(f"특허 {patent_app_number}의 상세 비교 완료")
            
        except Exception as e:
            print(f"특허 {patent.get('patent_application_number', '알 수 없음')} 상세 비교 중 오류: {str(e)}")
            # 스택 트레이스 출력 추가
            import traceback
            print(traceback.format_exc())
    
    print(f"상세 비교 완료: 특허 초안 ID {patent_draft_id}")
    return

async def perform_similarity_analysis(patent_draft_id: int, similarity_id: int):
    """특허 초안과 유사한 특허 분석 - 하이브리드 접근법 (TF-IDF로 필터링 후 BERT로 정밀 분석)"""
    print(f"유사도 분석 시작: 특허 초안 ID {patent_draft_id}, 유사도 ID {similarity_id}")
    
    try:
        # 특허 초안 조회
        draft_query = """
        SELECT * FROM patent_draft 
        WHERE patent_draft_id = :draft_id
        """
        draft = await database.fetch_one(
            query=draft_query,
            values={"draft_id": patent_draft_id}
        )
        
        if not draft:
            raise Exception("특허 초안을 찾을 수 없습니다.")
        
        now = datetime.now(timezone.utc)
        
        # STEP 1: 적합도 검사 수행
        print(f"적합도 검사 시작: 특허 초안 ID {patent_draft_id}")
        fitness_results = await perform_fitness_check(patent_draft_id, draft, now)
        print(f"적합도 검사 완료: 적합 여부: {fitness_results.get('is_corrected', False)}")
        
        # STEP 2: TF-IDF 기반 1차 유사도 검사
        print(f"TF-IDF 유사도 분석 시작: 특허 초안 ID {patent_draft_id}")
        all_tfidf_candidates = await perform_tfidf_similarity(patent_draft_id, draft)
        print(f"TF-IDF 유사도 분석 완료: {len(all_tfidf_candidates)}개 특허 처리")
        
        # STEP 3: BERT 기반 2차 유사도 검사 (상위 10개)
        print(f"BERT 유사도 분석 시작: 특허 초안 ID {patent_draft_id}")
        final_results = await perform_bert_similarity(patent_draft_id, draft, all_tfidf_candidates[:10])
        print(f"BERT 유사도 분석 완료: {len(final_results)}개 특허 선별")
        
        # STEP 4: 유사도 결과 저장
        print(f"유사도 결과 저장 시작: {len(final_results)}개 특허")
        await save_similarity_results(patent_draft_id, similarity_id, final_results, now)
        print(f"유사도 결과 저장 완료")
        
        # STEP 5: 상위 3개 특허에 대한 상세 비교 (KIPRIS API 연동)
        print(f"상세 비교 시작: 특허 초안 ID {patent_draft_id}")
        await perform_detailed_comparison(patent_draft_id, draft, final_results[:3], now)
        print(f"상세 비교 완료")
        
        return final_results
        
    except Exception as e:
        print(f"유사도 분석 중 오류 발생: {str(e)}")
        traceback.print_exc()  # 오류 스택 트레이스 출력
        return []

# 개선된 문맥 적합도 검사 함수
def improved_context_fitness(draft_dict, section_key, section_text):
    """개선된 문맥 적합도 검사 - 다중 패턴 및 주제 관련성 평가"""
    try:
        # 1. 섹션별 다중 패턴 예시 정의
        field_examples = {
            "technical_field": [
                "본 발명은 기술 분야에 관한 것으로", 
                "본 기술은 다음 분야와 관련된다", 
                "이 발명은 다음 영역에 속한다"
            ],
            "background": [
                "종래 기술에서는 다음과 같은 문제점이 있었다", 
                "기존 시스템은 다음과 같은 한계가 있다", 
                "현재 사용되는 방식은 여러 단점을 가지고 있다"
            ],
            "problem": [
                "본 발명이 해결하고자 하는 과제는", 
                "본 기술의 목적은", 
                "해결해야 할 문제는"
            ],
            "solution": [
                "상기 과제를 해결하기 위한 본 발명의 구성은", 
                "이 문제를 해결하기 위해", 
                "본 발명에서 제안하는 해결책은"
            ],
            "effect": [
                "본 발명에 따르면 다음과 같은 효과가 있다", 
                "본 기술의 장점은", 
                "이 발명의 효과로는"
            ],
            "summary": [
                "본 발명을 요약하면", 
                "요약하자면", 
                "본 기술은 다음과 같이 요약된다"
            ],
            "claim": [
                "청구항 1.", 
                "다음을 포함하는", 
                "특징으로 하는"
            ]
        }
        
        # 2. 섹션별 키워드 정의
        section_keywords = {
            "technical_field": ["분야", "기술", "관한", "관련된", "특히"],
            "background": ["기존", "종래", "문제", "한계", "단점", "현재"],
            "problem": ["과제", "해결", "목적", "문제"],
            "solution": ["구성", "수단", "방법", "구현", "포함"],
            "effect": ["효과", "장점", "개선", "향상", "감소"],
            "summary": ["요약", "정리", "줄이면", "기술"],
            "claim": ["청구항", "포함", "구성", "특징"]
        }
        
        # 3. 섹션 텍스트의 BERT 벡터 생성
        section_vector = get_bert_vector(section_text)
        
        # 4. 다중 패턴 접근법 - 가장 높은 유사도 점수 사용
        pattern_scores = []
        for example in field_examples.get(section_key, ["특허 문서 텍스트"]):
            example_vector = get_bert_vector(example)
            similarity = safe_cosine_similarity(section_vector, example_vector)
            pattern_scores.append(similarity)
        
        pattern_score = max(pattern_scores) if pattern_scores else 0.0
        
        # 5. 주제와의 관련성 평가
        title_text = draft_dict.get("patent_draft_title", "")
        title_vector = get_bert_vector(title_text)
        relevance_score = safe_cosine_similarity(title_vector, section_vector)
        
        # 6. 키워드 포함 여부 확인
        keywords = section_keywords.get(section_key, [])
        keyword_matches = sum(1 for keyword in keywords if keyword in section_text)
        keyword_score = min(1.0, keyword_matches / max(1, len(keywords)))
        
        # 7. 가중 평균으로 최종 점수 계산
        final_score = (0.4 * pattern_score) + (0.4 * relevance_score) + (0.2 * keyword_score)
        
        # 디버깅용
        print(f"섹션: {section_key}, 패턴 점수: {pattern_score:.2f}, 관련성 점수: {relevance_score:.2f}, 키워드 점수: {keyword_score:.2f}, 최종: {final_score:.2f}")
        
        return final_score
    except Exception as e:
        print(f"개선된 문맥 적합도 검사 중 오류: {str(e)}")
        return 0.0  # 오류 시 0점 반환

# 기존 함수는 대체됨
def check_context_fitness(text: str, field_type: str) -> float:
    """BERT 벡터를 활용한 문맥 적합도 검사 (레거시 함수 - 호환성 유지)"""
    try:
        # 텍스트의 BERT 벡터 추출
        vector = get_bert_vector(text)
        
        # 특허 필드별 예상 패턴 벡터
        field_examples = {
            "technical_field": "본 발명은 기술 분야에 관한 것으로, 특히 기술의 응용과 관련된다",
            "background": "종래 기술에서는 다음과 같은 문제점이 있었다",
            "problem": "본 발명이 해결하고자 하는 과제는",
            "solution": "상기 과제를 해결하기 위한 본 발명의 구성은",
            "effect": "본 발명에 따르면 다음과 같은 효과가 있다",
            "summary": "본 발명을 요약하면",
            "claim": "청구항 1. 다음을 포함하는 장치:"
        }
        
        example_text = field_examples.get(field_type, "특허 문서 텍스트")
        example_vector = get_bert_vector(example_text)
        
        # 안전한 코사인 유사도 계산
        similarity = safe_cosine_similarity(vector, example_vector)
        return similarity
    except Exception as e:
        print(f"문맥 적합도 검사 중 오류: {str(e)}")
        return 0.0  # 오류 시 0점 반환
