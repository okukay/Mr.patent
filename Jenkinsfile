pipeline {
    agent any
    
    environment {
        DOCKER_COMPOSE_DIR = '/home/ubuntu/mr_patent'
        BACKEND_IMAGE = 'mr_patent-backend'
        BRANCH_NAME = "${env.BRANCH_NAME}"
        DOCKER_COMPOSE = '/usr/local/bin/docker-compose'
    }
    
    stages {
        stage('Setup') {
            steps {
                echo '====== 환경 설정 시작 ======'
                // 도커 컴포즈 설치 확인 또는 설치
                sh '''
                    if ! command -v docker-compose &> /dev/null; then
                        echo "Docker Compose not found, installing..."
                        curl -L "https://github.com/docker/compose/releases/download/v2.24.3/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
                        chmod +x /usr/local/bin/docker-compose
                        # 심볼릭 링크 생성
                        ln -sf /usr/local/bin/docker-compose /usr/bin/docker-compose
                    fi
                    docker-compose --version
                '''
                echo '====== 환경 설정 완료 ======'
            }
        }
        
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

                // .env 복사
                sh 'cp /home/ubuntu/mr_patent/.env /home/ubuntu/mr_patent/.env || echo ".env not found, skipping..."'

                // 빌드 결과 복사
                sh 'mkdir -p /home/ubuntu/mr_patent/build/libs/'
                sh 'cp -f mr_patent_backend/build/libs/*.jar /home/ubuntu/mr_patent/build/libs/ || true'

                // Firebase 키 복사
                withCredentials([file(credentialsId: 'firebase_key', variable: 'FIREBASE_KEY_FILE')]) {
                    sh 'mkdir -p /home/ubuntu/mr_patent/config/firebase'
                    sh 'cp -f ${FIREBASE_KEY_FILE} /home/ubuntu/mr_patent/config/firebase/firebase-service-account.json'
                    sh 'chmod 600 /home/ubuntu/mr_patent/config/firebase/firebase-service-account.json'
                }

                // 디버깅 정보 출력
                sh 'echo "현재 작업 디렉토리 확인:" && pwd'
                sh 'echo ".env 파일 있는지 확인:" && ls -al /home/ubuntu/mr_patent/.env || echo "없음"'
                sh 'echo "docker-compose.yml 위치 확인:" && ls -al /home/ubuntu/mr_patent/docker-compose.yml || echo "없음"'

                // 도커 재배포
                sh '''
                    cd /home/ubuntu/mr_patent
                    docker-compose -f docker-compose.yml stop backend || true
                    docker-compose -f docker-compose.yml rm -f backend || true
                    docker-compose -f docker-compose.yml build --no-cache backend
                    docker-compose -f docker-compose.yml up -d --no-deps backend
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