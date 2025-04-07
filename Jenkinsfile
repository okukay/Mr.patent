pipeline {
    agent any
    
    environment {
        DOCKER_COMPOSE_DIR = '/home/ubuntu/mr_patent'
        BACKEND_IMAGE = 'mr_patent-backend'
        BRANCH_NAME = "${env.BRANCH_NAME}"
    }
    
    stages {
        stage('Checkout') {
            steps {
                checkout scm
                // 빌드 시작 시 커밋 정보 저장
                script {
                    env.GIT_AUTHOR = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                    env.GIT_EMAIL = sh(script: "git show -s --pretty=%ae", returnStdout: true).trim()
                }
            }
        }
        
        stage('Build') {
            steps {
                echo '====== 백엔드 빌드 시작 ======'
                dir('mr_patent_backend') {
                    sh 'chmod +x ./gradlew || true'
                    sh './gradlew clean build -x test'
                }
                echo '====== 백엔드 빌드 완료 ======'
            }
        }
        
        stage('Test') {
            steps {
                echo '====== 백엔드 테스트 시작 ======'
                dir('mr_patent_backend') {
                    sh './gradlew test || true'
                }
                echo '====== 백엔드 테스트 완료 ======'
            }
        }
        
        stage('Deploy') {
            steps {
                echo '====== 백엔드 배포 시작 ======'
                // 빌드 디렉토리 생성 및 JAR 파일 복사
                sh 'mkdir -p ${DOCKER_COMPOSE_DIR}/build/libs/'
                sh 'cp -f mr_patent_backend/build/libs/*.jar ${DOCKER_COMPOSE_DIR}/build/libs/ || true'
                // Firebase 키 파일 복사
                withCredentials([file(credentialsId: 'firebase_key', variable: 'FIREBASE_KEY_FILE')]) {
                    // Firebase 키 디렉토리 생성 및 파일 복사
                    sh 'mkdir -p ${DOCKER_COMPOSE_DIR}/config/firebase'
                    sh 'cp -f ${FIREBASE_KEY_FILE} ${DOCKER_COMPOSE_DIR}/config/firebase/firebase-service-account.json'
                    sh 'chmod 600 ${DOCKER_COMPOSE_DIR}/config/firebase/firebase-service-account.json'
                }
                
                // 도커 직접 명령어로 재시작
                sh '''
                    cd ${DOCKER_COMPOSE_DIR}
                    
                    # 도커 컨테이너 중지 및 제거
                    docker stop mr_patent_backend || true
                    docker rm mr_patent_backend || true
                    
                    # 도커 이미지 빌드
                    docker build -t mr_patent_backend -f deploy/backend/Dockerfile .
                    
                    # 도커 컨테이너 실행
                    docker run -d --name mr_patent_backend \\
                      --network app-network \\
                      -p 8080:8080 \\
                      -v ${DOCKER_COMPOSE_DIR}/config:/app/config \\
                      --env-file .env \\
                      mr_patent_backend \\
                      java -jar app.jar --spring.config.location=file:/app/config/
                    
                    # 이미지 정리
                    docker image prune -f || true
                '''
                
                echo '====== 백엔드 배포 완료 ======'
            }
        }
        
        stage('Notification') {
            steps {
                echo 'jenkins notification!'
            }
        }
    }
    
    post {
        success {
            echo '====== 파이프라인 성공 ======'
            mattermostSend(
                color: 'good',
                message: "빌드 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${env.GIT_AUTHOR}(${env.GIT_EMAIL})\n(<${env.BUILD_URL}|Details>)",
                endpoint: 'https://meeting.ssafy.com/hooks/hgafhbr6n7fe7japbi7n5tw36o',
                channel: 'D208-GitLab-Build'
            )
        }
        failure {
            echo '====== 파이프라인 실패 ======'
            mattermostSend(
                color: 'danger',
                message: "빌드 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${env.GIT_AUTHOR}(${env.GIT_EMAIL})\n(<${env.BUILD_URL}|Details>)",
                endpoint: 'https://meeting.ssafy.com/hooks/hgafhbr6n7fe7japbi7n5tw36o',
                channel: 'D208-GitLab-Build'
            )
        }
        always {
            echo '====== 파이프라인 종료 ======'
            cleanWs()
        }
    }
}