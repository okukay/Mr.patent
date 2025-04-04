package com.d208.mr_patent_backend.domain.voca.util;

import com.d208.mr_patent_backend.domain.voca.entity.Word;
import com.d208.mr_patent_backend.domain.voca.repository.WordRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataLoader {

    @Bean
    public CommandLineRunner initData(WordRepository wordRepository) {
        return args -> {
            // 데이터베이스에 이미 단어가 있는지 확인
            if (wordRepository.count() > 0) {
                System.out.println("단어 데이터가 이미 존재합니다. 초기화를 건너뜁니다.");
                return;
            }

            System.out.println("단어 데이터 초기화를 시작합니다...");
            LocalDateTime now = LocalDateTime.now();

            // 레벨 1 단어 (기본 개념 - 초보자를 위한 필수 용어)
            List<Word> level1Words = new ArrayList<>();
            level1Words.add(Word.builder().name("특허").mean("발명자가 일정 기간 동안 독점적으로 기술을 사용할 수 있도록 보호해주는 권리").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("출원").mean("특허를 받기 위해 특허청에 신청하는 절차").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("발명").mean("기존에 없던 기술이나 방법을 새롭게 만들어내는 것").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("권리").mean("법적으로 보호받을 수 있는 힘이나 자격").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("보호").mean("특허법에 의해 발명을 독점적으로 사용할 수 있도록 지켜주는 것").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("아이디어").mean("새로운 발명이나 기술의 기초가 되는 창의적인 생각").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("신청").mean("특허를 받기 위해 서류를 제출하는 과정").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("심사").mean("출원된 특허가 법적 요건을 충족하는지 평가하는 과정").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("등록").mean("심사를 통과한 후 특허로 공식 인정받는 절차").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("발명자").mean("새로운 기술이나 제품을 만들어낸 사람").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("법률").mean("특허를 보호하기 위해 만들어진 규칙과 제도").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("산업재산권").mean("특허, 상표, 디자인 등 산업과 관련된 지식재산권의 총칭").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("특허청").mean("특허 관련 서류를 심사하고 등록하는 정부 기관").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("지식재산권").mean("창작물(발명, 디자인, 상표 등)에 대한 법적 권리").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("거절").mean("특허가 법적 요건을 충족하지 못해 등록되지 않는 상태").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("공개").mean("출원된 특허가 일정 기간 후 일반에게 공개되는 것").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("심사관").mean("특허청에서 특허 신청을 검토하는 전문가").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("특허증").mean("특허 등록이 완료되었음을 증명하는 공식 문서").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("독점").mean("특정 기술을 다른 사람이 사용할 수 없도록 권리를 가지는 것").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("기술").mean("산업이나 생활에서 사용되는 전문적인 지식과 방법").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("제품").mean("발명을 통해 만들어진 물건 또는 서비스").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("기업").mean("제품이나 서비스를 생산·판매하는 조직").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("특허비용").mean("특허 출원, 심사 및 등록에 필요한 수수료").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("출원서").mean("특허를 신청할 때 작성하는 공식 서류").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("변리사").mean("특허 출원 및 법적 절차를 대행하는 전문가").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("발명의 명칭").mean("출원서에 기재하는 발명의 공식적인 제목").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("도면").mean("발명을 시각적으로 설명하는 그림").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("서류").mean("특허 출원을 위해 제출해야 하는 각종 문서").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("발명진흥법").mean("발명을 장려하고 보호하기 위한 법률").level((byte) 1).createdAt(now).updatedAt(now).build());
            level1Words.add(Word.builder().name("심사기간").mean("특허가 심사를 받는 데 걸리는 시간").level((byte) 1).createdAt(now).updatedAt(now).build());

            // 레벨 2 단어 (출원 과정 - 실무 이해를 위한 용어)
            List<Word> level2Words = new ArrayList<>();
            level2Words.add(Word.builder().name("특허 출원번호").mean("출원한 특허가 부여받는 고유한 식별 번호").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("출원일").mean("특허를 신청한 날짜").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("심사청구").mean("특허 심사를 받기 위해 공식적으로 요청하는 것").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("선행기술").mean("이미 존재하는 유사한 기술이나 발명").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("신규성").mean("기존에 없던 새로운 발명이어야 하는 기준").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("진보성").mean("기존 기술보다 발전된 기술이어야 하는 기준").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("실용신안").mean("특허보다는 보호기간이 짧지만 실용적인 기술을 보호하는 제도").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("디자인권").mean("제품의 외관(형태, 모양 등)에 대한 권리").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("상표권").mean("브랜드 이름이나 로고 등을 보호하는 권리").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("특허권자").mean("특허를 소유하고 있는 사람 또는 기업").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("청구항").mean("발명에서 보호받고자 하는 범위를 기술한 문장").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("명세서").mean("발명의 내용을 자세히 설명하는 문서").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("기술 설명").mean("발명의 원리와 작동 방식을 서술한 부분").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("심사 기준").mean("특허 등록 여부를 판단하는 공식적인 기준").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("심사 과정").mean("특허가 심사되는 절차와 단계").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("심사 청구료").mean("심사를 받기 위해 내는 비용").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("중간사건").mean("출원 후 심사 과정에서 발생하는 보완 요청이나 변경 사항").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("의견서").mean("심사관의 거절 이유에 대해 반박하는 서류").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("보정서").mean("출원 내용을 수정할 때 제출하는 문서").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("출원공개제도").mean("출원된 특허가 일정 기간 후 자동으로 공개되는 제도").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("국제출원").mean("여러 나라에서 동시에 특허를 출원하는 절차").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("PCT").mean("한 번의 출원으로 여러 나라에서 심사받을 수 있도록 하는 국제 조약").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("발명의 개요").mean("발명을 간략히 설명하는 부분").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("특허정보검색").mean("기존 특허를 검색하여 중복 여부를 확인하는 과정").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("지식재산센터").mean("특허 관련 상담 및 지원을 제공하는 기관").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("특허분쟁").mean("특허권과 관련된 법적 다툼").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("특허침해").mean("특허권자의 허락 없이 기술을 무단 사용하는 행위").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("특허출원비용").mean("특허 출원에 소요되는 전체 비용").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("출원 전략").mean("특허를 효과적으로 출원하기 위한 계획").level((byte) 2).createdAt(now).updatedAt(now).build());
            level2Words.add(Word.builder().name("공보").mean("공개된 특허 정보를 담은 문서").level((byte) 2).createdAt(now).updatedAt(now).build());

            // 레벨 3 단어 (출원 심화 - 특허 심사와 전략적 요소)
            List<Word> level3Words = new ArrayList<>();
            level3Words.add(Word.builder().name("출원 심사관").mean("특허청에서 출원된 특허를 심사하는 전문가").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("등록결정").mean("특허청이 특허를 인정하고 등록을 승인하는 결정").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("거절결정").mean("출원된 특허가 요건을 충족하지 못해 등록이 거부된 상태").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허무효").mean("등록된 특허가 법적 요건을 충족하지 못해 무효화되는 것").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허심판").mean("특허 관련 분쟁을 해결하기 위해 진행하는 재판 절차").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("정정심판").mean("등록된 특허의 오류를 수정하기 위해 진행하는 심판").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("거절이유통지서").mean("심사관이 특허 거절 사유를 설명한 공식 문서").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허청구항").mean("특허 보호 범위를 규정하는 조항").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("심사기준법령").mean("특허 심사를 위한 법적 기준과 규정").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("명세서 작성").mean("발명의 내용을 문서화하여 특허 출원을 준비하는 과정").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("선출원주의").mean("동일한 발명이라도 먼저 출원한 사람이 권리를 얻는 원칙").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허검색").mean("기존 특허를 검색하여 중복 여부를 확인하는 과정").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허소송").mean("특허 관련 분쟁이 법원에서 다뤄지는 절차").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("강제실시권").mean("특정 조건에서 정부가 강제로 특허를 사용하게 하는 권리").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("지식재산보호").mean("특허 및 기타 지식재산을 보호하는 법적 제도").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("침해경고장").mean("특허권자가 특허 침해자에게 보내는 경고 문서").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허양도").mean("특허권을 다른 사람에게 판매하거나 이전하는 것").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("실시권").mean("특허 기술을 사용할 수 있는 법적 권리").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("전용실시권").mean("특정 개인이나 기업이 독점적으로 특허를 사용할 수 있는 권리").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("통상실시권").mean("여러 명이 동시에 특허 기술을 사용할 수 있는 권리").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("실시의무").mean("특허권자가 특허를 실제로 활용해야 하는 의무").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("라이선스 계약").mean("특허권자가 다른 사람에게 사용 허가를 주는 계약").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허권 존속기간").mean("특허권이 유지되는 기간(출원 후 최대 20년)").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허수수료").mean("특허 출원 및 등록을 위해 지불하는 비용").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("권리범위확인심판").mean("특허권의 보호 범위를 명확히 하기 위한 심판").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("무효심판청구").mean("기존 특허가 무효임을 주장하는 심판 청구").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허맵").mean("특허 동향을 분석하여 사업 전략을 수립하는 도구").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허전략").mean("특허를 효과적으로 활용하기 위한 기업의 전략").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("연구개발").mean("새로운 기술이나 제품을 개발하는 활동").level((byte) 3).createdAt(now).updatedAt(now).build());
            level3Words.add(Word.builder().name("특허기술이전").mean("개발된 특허 기술을 다른 기업이나 기관에 이전하는 것").level((byte) 3).createdAt(now).updatedAt(now).build());

            // 레벨 4 단어 (국제특허 및 심판 - 고급 개념)
            List<Word> level4Words = new ArrayList<>();
            level4Words.add(Word.builder().name("특허분할출원").mean("하나의 출원을 여러 개로 나누어 출원하는 방식").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("보정제한").mean("심사 과정에서 출원 내용을 변경할 수 있는 범위 제한").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허심사하이웨이(PPH)").mean("다른 나라에서 신속한 특허 심사를 받기 위한 제도").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("조기공개제도").mean("출원된 특허를 일반에 빨리 공개하는 제도").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("소프트웨어 특허").mean("소프트웨어 기술에 대해 부여되는 특허").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("바이오 특허").mean("생명공학 관련 기술에 대한 특허").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("등록유지료").mean("특허 등록 후 일정 기간마다 지불해야 하는 유지 비용").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허동향분석").mean("현재 시장에서 특허 기술이 어떻게 변화하고 있는지 분석하는 것").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허침해소송").mean("특허권자가 특허를 침해한 상대방을 상대로 제기하는 소송").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허심판원").mean("특허 관련 심판을 진행하는 기관").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허심사절차").mean("출원 후 등록까지 거치는 전체 심사 과정").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("선사용권").mean("출원 전에 특정 기술을 사용하고 있었다면 계속 사용할 수 있는 권리").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("부경법(부정경쟁방지법)").mean("특허권 침해 외에도 부정한 경쟁을 막기 위한 법률").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("실질심사제도").mean("특허가 법적 요건을 충족하는지 꼼꼼히 심사하는 방식").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("임시명세서").mean("출원 전에 기술 내용을 정리한 초안 문서").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("직무발명").mean("회사에서 근무 중 발명한 경우, 권리가 회사에 귀속되는 발명").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허전략수립").mean("기업이 특허를 활용하여 경쟁력을 확보하는 전략").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허유사도분석").mean("기존 특허와 비교하여 유사성을 분석하는 방법").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허출원동향").mean("특정 기술 분야에서 특허 출원이 얼마나 활발한지 분석하는 것").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허권 제한").mean("특허권자가 권리를 행사할 수 없는 특정 조건").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("미국특허청(USPTO)").mean("미국의 특허 심사 기관").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("유럽특허청(EPO)").mean("유럽에서 특허를 관리하는 기관").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("중국특허청(CNIPA)").mean("중국의 특허청").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("국제조사보고서").mean("PCT 출원 시 국제조사기관에서 제공하는 보고서").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("PCT 국제예비심사").mean("PCT 출원 후 각국 특허청에서 진행하는 심사").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("다국적 출원전략").mean("여러 나라에서 효과적으로 특허를 등록하는 전략").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("국가별 출원제도 차이").mean("국가마다 다른 특허 심사 방식과 등록 절차").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("공동출원").mean("여러 명이 공동으로 특허를 출원하는 방식").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("특허권이전계약").mean("특허를 다른 사람에게 양도하는 계약").level((byte) 4).createdAt(now).updatedAt(now).build());
            level4Words.add(Word.builder().name("기술이전계약").mean("특허 기술을 이전할 때 체결하는 계약").level((byte) 4).createdAt(now).updatedAt(now).build());

            // 레벨 5 단어 (전문가용 - 국제 특허 및 고급 전략)
            List<Word> level5Words = new ArrayList<>();
            level5Words.add(Word.builder().name("특허청구항 해석").mean("특허 보호 범위를 결정하기 위해 청구항의 의미를 분석하는 과정").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("출원공개 vs 등록공개").mean("출원공개는 출원 후 18개월이 지나면 자동 공개되며, 등록공개는 특허가 등록된 후 공개되는 것").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("출원우선권 주장").mean("동일한 발명을 먼저 출원한 것을 인정받아 후속 출원에서 유리한 지위를 확보하는 것").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("PCT 국제출원").mean("하나의 출원으로 여러 나라에서 동시에 특허 심사를 받을 수 있는 제도").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("EPC(유럽특허조약)").mean("유럽에서 단일 특허 출원을 가능하게 하는 조약").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허침해 감정").mean("법원이 특허 침해 여부를 판단하기 위해 전문가 감정을 받는 절차").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허청구항 보정 전략").mean("특허 심사 과정에서 청구항을 보정하여 등록 가능성을 높이는 전략").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허회피설계(디자인 어라운드)").mean("기존 특허를 침해하지 않도록 우회하는 기술적 설계 방법").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허무효소송").mean("이미 등록된 특허가 무효임을 주장하며 법적으로 다투는 소송").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허권 소멸").mean("존속기간 만료, 무효 판정, 또는 수수료 미납으로 인해 특허권이 소멸하는 것").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허법 개정 사항").mean("특허법이 변경될 경우, 기존 특허권자와 신규 출원자에게 미치는 영향").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 실시보상제도").mean("직무발명을 한 직원에게 기업이 보상을 제공하는 제도").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("기업 특허 포트폴리오 전략").mean("기업이 다수의 특허를 확보하여 기술 경쟁력을 강화하는 전략").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 클러스터링").mean("관련된 특허들을 묶어서 연구개발 및 보호 전략을 수립하는 기법").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허공유(Open Patent)").mean("특정 기술을 특허로 등록한 후 누구나 사용할 수 있도록 공개하는 방식").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("프라이어 아트(Prior Art) 분석").mean("신규성 판단을 위해 기존 기술을 조사하는 과정").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 풀(Patent Pool)").mean("여러 기업이 특허를 공유하여 특정 기술을 공동으로 사용하는 제도").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("표준특허(Standard Essential Patent, SEP)").mean("특정 산업 표준을 따르는 기술에 반드시 필요한 특허").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("FRAND 원칙").mean("표준특허 사용 시 공정하고 합리적인 조건으로 라이선스를 제공해야 한다는 원칙").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("트롤 특허(Patent Troll)").mean("특허를 직접 활용하지 않고 소송을 통해 이익을 얻는 기업 또는 개인").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("비즈니스 모델 특허(BM 특허)").mean("온라인 서비스, 금융, 마케팅 등의 사업 모델을 보호하는 특허").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("생명공학 특허 논란").mean("바이오 특허가 윤리적 문제와 충돌할 가능성에 대한 논쟁").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("의약품 특허 만료 및 제네릭").mean("특허가 만료된 후 제약사들이 유사 의약품(제네릭)을 출시하는 과정").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허연장제도(PTE)").mean("신약 등 특정 특허의 보호 기간을 연장하는 제도").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("기술표준과 특허의 관계").mean("국제 기술표준이 정해질 때 관련 특허가 어떻게 영향을 미치는지에 대한 개념").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 인프린지먼트(Patent Infringement)").mean("특허권 침해와 관련된 법적 책임과 사례").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("에퀴티 라이선싱(Equity Licensing)").mean("특허 기술을 제공하는 대신 회사 지분을 받는 방식").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("크로스 라이선싱(Cross Licensing)").mean("서로 다른 회사들이 특허를 교환하여 사용 허가를 주는 협약").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허의 기술적 기여도 분석").mean("특정 특허가 산업과 기술 발전에 기여한 정도를 평가하는 과정").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("산업재산권(IPR)과 특허의 관계").mean("특허가 포함된 산업재산권(IPR) 체계 및 관련 법률").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 벤치마킹").mean("경쟁사의 특허 전략을 분석하여 자사 특허 전략을 세우는 방법").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("특허 포트폴리오 최적화").mean("보유 특허 중 불필요한 특허를 정리하고 핵심 특허를 강화하는 전략").level((byte) 5).createdAt(now).updatedAt(now).build());
            level5Words.add(Word.builder().name("미래 기술 예측과 특허 출원").mean("향후 유망 기술을 예측하고 선제적으로 특허를 출원하는 전략").level((byte) 5).createdAt(now).updatedAt(now).build());

            // 단어 저장
            wordRepository.saveAll(level1Words);
            // level2Words, level3Words (생략)
            wordRepository.saveAll(level4Words);
            wordRepository.saveAll(level5Words);

            System.out.println("단어 데이터 초기화를 완료했습니다.");
        };
    }
}