#include <Servo.h>

// create object of brushless motor to control each motor
Servo motors[4];
int count =0;
void setup(){
  //start the Serial connection
  Serial.begin(9600);
  pinMode(13,OUTPUT);
  digitalWrite(13,LOW);

  motors[0].attach(3);
  motors[1].attach(5);
  motors[2].attach(6);
  motors[3].attach(9);
}


void loop(){

  // Serial.write("start!!!");
  if (count==0){
    motors[0].writeMicroseconds(1000);
    motors[1].writeMicroseconds(1000);
    motors[2].writeMicroseconds(1000);
    motors[3].writeMicroseconds(1000);
    count++;
  }
  
  if (Serial.available()==4){
    // int array carry wanted throttle from each motor
    int motor_throttle [4];
   
    for (int i =0 ; i<4 ; i++){
    // read input from serial
      int throttle = Serial.read();
      //motor_throttle[i]= Serial.parseInt();
      motor_throttle[i]=map(throttle,0,200,1000,2000);
      motors[i].writeMicroseconds(motor_throttle[i]); 
    }
  }
 // Serial.println(Serial.available());
}




