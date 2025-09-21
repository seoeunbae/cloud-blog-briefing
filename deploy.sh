REPOSITORY=cloud-mail

cd $REPOSITORY

DIRECTORY= ls

if [ ! -z $DIRECTORY ]; then
    rm -rf $REPOSITORY/*
fi

cd $REPOSITORY/

echo "> Git Pull"

git pull

echo "> 프로젝트 Build 시작"

./gradlew clean build -x test

echo "> Build 파일 복사"

cp ~/$REPOSITORY/build/libs/*.jar ~/$REPOSITORY/

echo "> 현재 구동중인 애플리케이션 pid 확인"

CURRENT_PID=$(pgrep -f cloudblog_mailing)

echo "$CURRENT_PID"

if [ -z $CURRENT_PID ]; then
    echo "> 현재 구동중인 애플리케이션이 없으므로 종료하지 않습니다."
else
    echo "> kill -2 $CURRENT_PID"
    kill -9 $CURRENT_PID
    sleep 5
fi

echo "> 새 어플리케이션 배포"

JAR_NAME=$(ls ~/$REPOSITORY/ | grep 'cloudblog_mailing' | tail -n 1)

echo "> nohup을 위한 권한 755 부여"

sudo chmod 755 ~/$REPOSITORY/cloudblog_mailing.jar

echo "> JAR Name: $JAR_NAME"

nohup java -jar ~/$REPOSITORY/$JAR_NAME > /dev/null 2> /dev/null < /dev/null &

nohup java -jar cloudblog_mailing.jar > /dev/null 2> /dev/null < /dev/null &
