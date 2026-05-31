// Требуется наличие следующих переменных в Jenkins:
// - SSH_CRED_ID - идентификатор SSH ключа
// - MERCHANT_CONTROL_DEPLOY_PATH - путь на сервере, куда необходимо расположить собранные проекты
// - MERCHANT_CONTROL_DEPLOY_HOST - IP адрес сервера, на который будут отправлены проекты
// - SSH_USER - пользователь SSH
// - SSH_PORT - порт SSH
pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'chmod +x gradlew'
                sh './gradlew clean bootJar --no-daemon'
            }
        }
        stage('Deploy') {
            steps {
                sshagent([env.SSH_CRED_ID]) {
                    sh "scp -P ${SSH_PORT} build/libs/merchant-control.jar ${SSH_USER}@${MERCHANT_CONTROL_DEPLOY_HOST}:${MERCHANT_CONTROL_DEPLOY_PATH}/"
                    sh "ssh -p ${SSH_PORT} ${SSH_USER}@${MERCHANT_CONTROL_DEPLOY_HOST} 'cd /srv/merchant-control && docker rollout --wait 60 --timeout 60 merchant-control'"
                }
            }
        }
    }

    post {
        success {
            echo 'Сборка и деплой успешно завершены!'
        }
        failure {
            echo 'Ошибка при сборке или деплое.'
        }
    }
}
