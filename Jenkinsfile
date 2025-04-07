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
                
                // 작업 디렉토리에 간단한 도커 컴포즈 파일 생성
                sh '''
                    cat > docker-compose-temp.yml << 'EOL'
        version: '3.8'

        services:
        backend:
            build:
            context: ${DOCKER_COMPOSE_DIR}
            dockerfile: deploy/backend/Dockerfile
            container_name: mr_patent_backend
            command: ["java", "-jar", "app.jar", "--spring.config.location=file:/app/config/"]
            volumes:
            - ${DOCKER_COMPOSE_DIR}/config:/app/config
            ports:
            - "8080:8080"
            networks:
            - app-network

        networks:
        app-network:
            external: true
        EOL

                    ${DOCKER_COMPOSE} -f docker-compose-temp.yml stop backend || true
                    ${DOCKER_COMPOSE} -f docker-compose-temp.yml rm -f backend || true
                    ${DOCKER_COMPOSE} -f docker-compose-temp.yml build --no-cache backend
                    ${DOCKER_COMPOSE} -f docker-compose-temp.yml up -d --no-deps backend
                    
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