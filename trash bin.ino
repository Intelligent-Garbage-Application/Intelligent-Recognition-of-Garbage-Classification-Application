#include <Servo.h>
#include <SoftwareSerial.h>
#include <DFPlayer_Mini_Mp3.h>

//创建四个垃圾桶引脚对象
Servo servo_4;
Servo servo_5;
Servo servo_6;
Servo servo_7;

//设置mp3模块的串口引脚
SoftwareSerial mySerial(2, 3);

//
void setup () {
  
  //设置4路舵机的引脚
  servo_4.attach(4);
  servo_5.attach(5);
  servo_6.attach(6);
  servo_7.attach(7);

  //设置舵机的初始角度
  servo_4.write(0);
  servo_5.write(0);
  servo_6.write(0);
  servo_7.write(0);

  //设置串口通讯
  Serial.begin (9600);
  mySerial.begin (9600);
  mp3_set_serial (mySerial);  
  delay(1);  
  mp3_set_volume (100);//设置mp3音量
}



void loop () {        
   int val = Serial.read();//定义变量，并把串口值赋给猪哥变量
   if (val == 1) {
      mp3_play_physical (1);//播放第1个声音文件
    digitalWrite(3,LOW);
    servo_4.write(70);   //设置舵机转动角度
    delay(2000);
    servo_4.write(0);    //设置舵机回到初始角度
    digitalWrite(3,HIGH);
  }
  if (val == 2) {
    mp3_play_physical (2);
    digitalWrite(3,LOW);
    servo_5.write(70);
    delay(2000);
    servo_5.write(0);
    digitalWrite(3,HIGH);
  }
  if (val == 3) {
    mp3_play_physical (3);
    digitalWrite(3,LOW);
    servo_6.write(70);
    delay(2000);
    servo_6.write(0);
    digitalWrite(3,HIGH);
  }
  if (val == 4) {
    mp3_play_physical (4);
    digitalWrite(3,LOW);
    servo_7.write(70);
    delay(2000);
    servo_7.write(0);
    digitalWrite(3,HIGH);
  }
}