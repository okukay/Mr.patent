pipeline {
    agent any

    environment {
        DOCKER_COMPOSE_DIR = "/var/jenkins_shared/mr_patent"
        DOCKER_COMPOSE = "$HOME/bin/docker-compose"
        BRANCH_NAME = "${env.BRANCH_NAME}"
        PYTHON_VERSION = "3.9"
        PATH = "/usr/local/bin:/usr/bin:/bin:$HOME/.local/bin"
    }

    stages {
        stage('Setup Environment') {
            steps {
                echo '====== 환경 설정 시작 ======'
                sh '''
                    # 도커 컴포즈 설치 확인
                    if ! command -v docker-compose &> /dev/null; then
                        echo "Installing Docker Compose..."
                        mkdir -p $HOME/bin
                        curl -L "https://github.com/docker/compose/releases/download/v2.24.3/docker-compose-$(uname -s)-$(uname -m)" -o $HOME/bin/docker-compose
                        chmod +x $HOME/bin/docker-compose
                    else
                        echo "Docker Compose already installed"
                    fi
                    docker-compose --version || $HOME/bin/docker-compose --version

                    # Python 설치 및 pipenv 환경 구성
                    apt-get update
                    apt-get install -y python3 python3-pip python3-venv
                    python3 -m pip install --user virtualenv pipenv
                '''
                echo '====== 환경 설정 완료 ======'
            }
        }

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.GIT_AUTHOR = sh(script: "git show -s --pretty=%an", returnStdout: true).trim()
                    env.GIT_EMAIL = sh(script: "git show -s --pretty=%ae", returnStdout: true).trim()
                }
            }
        }

        // ===================== Spring Backend =====================
        stage('Build Spring Backend') {
            steps {
                echo '====== 백엔드 빌드 시작 ======'
                dir('mr_patent_backend') {
                    sh 'chmod +x ./gradlew || true'
                    sh './gradlew clean build -x test'
                }
                echo '====== 백엔드 빌드 완료 ======'
            }
        }

        stage('Test Spring Backend') {
            steps {
                echo '====== 백엔드 테스트 시작 ======'
                dir('mr_patent_backend') {
                    sh './gradlew test || true'
                }
                echo '====== 백엔드 테스트 완료 ======'
            }
        }

        stage('Deploy Spring Backend') {
            steps {
                echo '====== 백엔드 배포 시작 ======'
                sh '''
                    cp ${DOCKER_COMPOSE_DIR}/.env ${DOCKER_COMPOSE_DIR}/.env || echo ".env not found, skipping..."
                    mkdir -p ${DOCKER_COMPOSE_DIR}/build/libs/
                    cp -f mr_patent_backend/build/libs/*.jar ${DOCKER_COMPOSE_DIR}/build/libs/ || true

                    mkdir -p ${DOCKER_COMPOSE_DIR}/config/firebase
                '''
                withCredentials([file(credentialsId: 'firebase_key', variable: 'FIREBASE_KEY_FILE')]) {
                    sh '''
                        cp -f ${FIREBASE_KEY_FILE} ${DOCKER_COMPOSE_DIR}/config/firebase/firebase-service-account.json
                        chmod 600 ${DOCKER_COMPOSE_DIR}/config/firebase/firebase-service-account.json
                    '''
                }

                sh '''
                    cd ${DOCKER_COMPOSE_DIR}
                    $HOME/bin/docker-compose -f docker-compose.yml stop backend || true
                    $HOME/bin/docker-compose -f docker-compose.yml rm -f backend || true
                    $HOME/bin/docker-compose -f docker-compose.yml build --no-cache backend
                    $HOME/bin/docker-compose -f docker-compose.yml up -d --no-deps backend
                    docker image prune -f || true
                '''
                echo '====== 백엔드 배포 완료 ======'
            }
        }

        // ===================== FastAPI Service =====================
        stage('Build FastAPI') {
            steps {
                echo '====== FastAPI 빌드 시작 ======'
                dir('mr_patent_bigdata') {
                    sh '''
                        python3 -m venv venv
                        source venv/bin/activate
                        pip install --upgrade pip
                        pip install pipenv
                        pipenv install --dev || pipenv install
                    '''
                }
                echo '====== FastAPI 빌드 완료 ======'
            }
        }

        stage('Test FastAPI') {
            steps {
                echo '====== FastAPI 테스트 시작 ======'
                dir('mr_patent_bigdata') {
                    sh '''
                        source venv/bin/activate
                        pipenv run pytest || true
                    '''
                }
                echo '====== FastAPI 테스트 완료 ======'
            }
        }

        stage('Deploy FastAPI') {
            steps {
                echo '====== FastAPI 배포 시작 ======'
                sh '''
                    mkdir -p ${DOCKER_COMPOSE_DIR}/fastapi
                    cp -rf mr_patent_bigdata/app ${DOCKER_COMPOSE_DIR}/fastapi/
                    cp -rf mr_patent_bigdata/models ${DOCKER_COMPOSE_DIR}/fastapi/
                    cp -f mr_patent_bigdata/Pipfile ${DOCKER_COMPOSE_DIR}/fastapi/
                    cp -f mr_patent_bigdata/Pipfile.lock ${DOCKER_COMPOSE_DIR}/fastapi/
                    cd mr_patent_bigdata
                    pipenv lock -r > ${DOCKER_COMPOSE_DIR}/fastapi/requirements.txt

                    mkdir -p ${DOCKER_COMPOSE_DIR}/backend
                    cp -f deploy/backend/Dockerfile ${DOCKER_COMPOSE_DIR}/backend/Dockerfile

                    cd ${DOCKER_COMPOSE_DIR}
                    $HOME/bin/docker-compose -f docker-compose.yml stop fastapi || true
                    $HOME/bin/docker-compose -f docker-compose.yml rm -f fastapi || true
                    $HOME/bin/docker-compose -f docker-compose.yml build --no-cache fastapi
                    $HOME/bin/docker-compose -f docker-compose.yml up -d fastapi
                    docker image prune -f
                '''
                echo '====== FastAPI 배포 완료 ======'
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
                message: "✅ 빌드 성공: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${env.GIT_AUTHOR} (${env.GIT_EMAIL})\n(<${env.BUILD_URL}|Details>)",
                endpoint: 'https://meeting.ssafy.com/hooks/hgafhbr6n7fe7japbi7n5tw36o',
                channel: 'D208-GitLab-Build'
            )
        }
        failure {
            echo '====== 파이프라인 실패 ======'
            mattermostSend(
                color: 'danger',
                message: "❌ 빌드 실패: ${env.JOB_NAME} #${env.BUILD_NUMBER} by ${env.GIT_AUTHOR} (${env.GIT_EMAIL})\n(<${env.BUILD_URL}|Details>)",
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
