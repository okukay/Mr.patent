# Mr.Patent - 특허 출원이 처음인 사람을 위한 도우미 앱

>***특허씨 Mr_Patent*** 는
>처음 특허를 준비하는 사람도, 지식재산이 낯선 사람도
문서 업로드, 유사도 판별, 용어 학습은 물론 전문가에게 상담까지 받을 수 있는 특허 출원 도우미 앱입니다.


## 🗂️ 목차
- [💪 Mr_Patent 서비스 소개](#-Mr_Patent-서비스-소개)<br/>
- [⏰ 개발 기간](#-개발-기간)<br/>
- [💡 기획 배경](#-기획-배경)<br/>
- [🎯 목표 및 주요 기능](#-목표-및-주요-기능)<br/>
- [🔧 기능 소개](#-기능-소개)<br/>
- [📢 기술 스택 소개](#-기술-스택-소개)<br/>
- [🔍 시스템 아키텍처](#-시스템-아키텍처)<br/>
- [💾 ERD 다이어그램](#-erd-다이어그램)<br/>
- [👥 팀 소개 및 역할](#-팀-소개-및-역할)<br/>

📝 [회의록 보기](https)  <br/>

## ⏰ 개발 기간
 2025.03.03 ~ 2025.04.11 (6주)

## 💡 기획 배경
>누구나 아이디어 하나쯤은 갖고 있지만,  
>막상 **특허 출원**을 시도하려고 하면 복잡한 용어와 절차에 막히기 쉽습니다.
>
>“비슷한 특허는 이미 있지 않을까?”  
>“출원 절차는 어떻게 시작하지?”  
>“변리사에게 바로 문의해도 될까?”
>
>이처럼 수많은 궁금증과 막막함 속에서  
>많은 사람들이 첫걸음을 내딛지 못하는 현실을 마주하게 됩니다.
>바로 이런 초보자들을 위해 기획되었습니다.

## 🎯 목표 및 주요 기능

#### 1. 누구나 쉽게 시작할 수 있는 특허 출원 지원
- 유사 특허 검색 및 유사도 분석석
- OCR 기반 문서 인식 및 구성 항목 자동 추출

#### 2. 특허 초보자를 위한 맞춤형 학습 & 진입 장벽 해소
- 용어 사전 및 개념 설명 카드 제공
- 퀴즈 기반 특허 용어 학습 기능

#### 3. 전문가 연결을 통한 실질적 출원 지원
- 전문가와 1:1 상담 기능 제공

## 🔧 기능소개

- 😀 **회원관리**

- 📄 **특허 문서 업로드 및 자동 분석**  
  PDF 특허 문서를 업로드하면 OCR로 구성 항목을 추출하고,유사 특허를 찾아 유사도를 비교합니다.

- 📚 **특허 용어 학습 기능**  
  어려운 특허 용어를 카드 형태로 학습할 수 있으며, 퀴즈를 통해 반복 학습도 가능합니다.

- 👩‍💼 **변리사 상담 신청**  
   직접 변리사에게 질문하고 피드백을 받을 수 있는 상담이 가능합니다.



## 📢 기술 스택 소개

### ⚙️ Tech Stack

#### 📱 Android
<img src="https://img.shields.io/badge/kotlin-%237F52FF.svg?style=for-the-badge&logo=kotlin&logoColor=white"> <img src="https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white"> <img src="https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white">

#### 🔧 Backend
<img src="https://img.shields.io/badge/java17-007396?style=for-the-badge&logo=java&logoColor=white"> <img src="https://img.shields.io/badge/springboot-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"> <img src="https://img.shields.io/badge/Spring%20Cloud%20AWS-FF9900?style=for-the-badge&logo=amazonaws&logoColor=white"> <img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"> <img src="https://img.shields.io/badge/socket.io-010101?style=for-the-badge&logo=socket.io&logoColor=white"> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"> <img src="https://img.shields.io/badge/FastAPI-005571?style=for-the-badge&logo=fastapi"> <img src="https://img.shields.io/badge/Apache%20Spark-FDEE21?style=flat-square&logo=apachespark&logoColor=black">



#### 🏗️ Build & Deployment
<img src="https://img.shields.io/badge/gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white"> <img src="https://img.shields.io/badge/docker-%230db7ed.svg?style=for-the-badge&logo=docker&logoColor=white"> <img src="https://img.shields.io/badge/jenkins-%D24939.svg?style=for-the-badge&logo=jenkins&logoColor=white">

#### 🗄️ Database & Cache
<img src="https://img.shields.io/badge/mysql-4479A1?style=for-the-badge&logo=mysql&logoColor=white"> 

#### ☁️ Infrastructure & Cloud
<img src="https://img.shields.io/badge/Amazon%20EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"> <img src="https://img.shields.io/badge/Amazon%20S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"> <img src="https://img.shields.io/badge/Ubuntu-E95420?style=for-the-badge&logo=ubuntu&logoColor=white">
<img src="https://img.shields.io/badge/nginx-%23009639.svg?style=for-the-badge&logo=nginx&logoColor=white"> <img src="https://img.shields.io/badge/Let's%20Encrypt-003A70?style=for-the-badge&logo=letsencrypt&logoColor=white">

#### 📡 External Services & APIs
<img src="https://img.shields.io/badge/firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=white"> <img src="https://img.shields.io/badge/FCM-FFCA28?style=for-the-badge&logo=firebase&logoColor=white">

#### 🔍 Monitoring & Tools
<img src="https://img.shields.io/badge/GitLab-FCA326?style=for-the-badge&logo=gitlab&logoColor=white"> <img src="https://img.shields.io/badge/Notion-000000?style=for-the-badge&logo=notion&logoColor=white"> <img src="https://img.shields.io/badge/Figma-F24E1E?style=for-the-badge&logo=figma&logoColor=white">


<br/>

## 🔍 시스템 아키텍처
<div align="center">

</div>

<br/>

## 💾 ERD 다이어그램

<div align="center">

</div>

<br/>

## 👥 팀 소개 및 역할

| Android | Android | Backend | Backend | CI/CD  | BigData |
|---------|---------|---------|---------|--------|---------|
| 수정    | 수미    | 동욱    | 정모    | 예지   | 용성    |




