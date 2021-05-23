#include <SoftwareSerial.h>
#define GpsSerial  Serial
#define DebugSerial Serial
int L = 13; //LED
SoftwareSerial kk= SoftwareSerial(10, 11);//BLUE
struct
{
  char GPS_Buffer[80];
  bool isGetData;   
  bool isParseData; 
  char UTCTime[11];   
  char latitude[11];    
  char N_S[2];  
  char longitude[12];   
  char E_W[2];   
  bool isUsefull;   
} Save_Data;

const unsigned int gpsRxBufferLength = 600;
char gpsRxBuffer[gpsRxBufferLength];
unsigned int ii = 0;


void setup()  //初始化内容
{
  GpsSerial.begin(9600);    
  DebugSerial.begin(9600);
  kk.begin(9600);
  DebugSerial.println("Wating...");
  kk.println("Working");
  Save_Data.isGetData = false;
  Save_Data.isParseData = false;
  Save_Data.isUsefull = false;
}

void loop()   
{ 
    gpsRead();  
    parseGpsBuffer();
    printGpsBuffer();

}

void errorLog(int num)
{
  kk.print("ERROR");
  kk.println(num);
  while (1)
  {
    digitalWrite(L, HIGH);
    delay(300);
    digitalWrite(L, LOW);
    delay(300);
  }
}

void printGpsBuffer()
{
  if (Save_Data.isParseData)
  {
    Save_Data.isParseData = false;

    if(Save_Data.isUsefull)
    {
      Save_Data.isUsefull = false;
      kk.print("latitude = ");
      kk.println(Save_Data.latitude);
      kk.print("longitude = ");
      kk.println(Save_Data.longitude);
    }
    else
    {
      kk.println("GPS DATA is not usefull!");
    }
    
  }
}

void parseGpsBuffer()
{
  char *subString;
  char *subStringNext;
  if (Save_Data.isGetData)
  {
    Save_Data.isGetData = false;

    
    for (int i = 0 ; i <= 6 ; i++)
    {
      if (i == 0)
      {
        if ((subString = strstr(Save_Data.GPS_Buffer, ",")) == NULL)
          errorLog(1);
      }
      else
      {
        subString++;
        if ((subStringNext = strstr(subString, ",")) != NULL)
        {
          char usefullBuffer[2]; 
          switch(i)
          {
            case 1:memcpy(Save_Data.UTCTime, subString, subStringNext - subString);break; 
            case 2:memcpy(usefullBuffer, subString, subStringNext - subString);break; 
            case 3:memcpy(Save_Data.latitude, subString, subStringNext - subString);
            case 4:memcpy(Save_Data.N_S, subString, subStringNext - subString);break;
            case 5:memcpy(Save_Data.longitude, subString, subStringNext - subString);break; 
            case 6:memcpy(Save_Data.E_W, subString, subStringNext - subString);break;

            default:break;
          }

          subString = subStringNext;
          Save_Data.isParseData = true;
          if(usefullBuffer[0] == 'A')
            Save_Data.isUsefull = true;
          else if(usefullBuffer[0] == 'V')
            Save_Data.isUsefull = false;

        }
        else
        {
          errorLog(2); 
        }
      }


    }
  }
}


void gpsRead() {
  while (GpsSerial.available())
  {
    gpsRxBuffer[ii++] = GpsSerial.read();
    if (ii == gpsRxBufferLength)clrGpsRxBuffer();
  }

  char* GPS_BufferHead;
  char* GPS_BufferTail;
  if ((GPS_BufferHead = strstr(gpsRxBuffer, "$GPRMC,")) != NULL || (GPS_BufferHead = strstr(gpsRxBuffer, "$GNRMC,")) != NULL )
  {
    if (((GPS_BufferTail = strstr(GPS_BufferHead, "\r\n")) != NULL) && (GPS_BufferTail > GPS_BufferHead))
    {
      memcpy(Save_Data.GPS_Buffer, GPS_BufferHead, GPS_BufferTail - GPS_BufferHead);
      Save_Data.isGetData = true;

      clrGpsRxBuffer();
    }
  }
}

void clrGpsRxBuffer(void)
{
  memset(gpsRxBuffer, 0, gpsRxBufferLength);   
  ii = 0;
}