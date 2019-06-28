

C:\jdk1.8.0_181\bin\keytool -genkey -keyalg RSA -alias mtspredbot -keystore mtspredbotjks.jks -storepass XXXXXXXXXXXXXXX -validity 360 -keysize 2048
C:\jdk1.8.0_181\bin\keytool -importkeystore -srckeystore mtspredbotjks.jks -destkeystore mtspredbotkcs.p12 -srcstoretype jks -deststoretype pkcs12
C:\openssl\openssl pkcs12 -in C:\tcert\mtspredbotkcs.p12 -out C:\tcert\mtspredbot.pem -nokeys

--------------------------------------------------------------------------------------
ngrok authkey XXXXXXXXXXXXXXXXXXXX
Authtoken saved to configuration file: C:\Users\Administrator/.ngrok2/ngrok.yml
ngrok http 8443

Session Status                online
Account                        (Plan: Free)
Version                       2.3.30
Region                        United States (us)
Web Interface                 http://127.0.0.1:4040
Forwarding                    https://59913958.ngrok.io -> http://localhost:8443
--------------------------------------------------------------------------------------

  val baseUrl = "http://VDS ip:8443"
  val client = new AkkaHttpClient(botToken)
  override val port = 8443
  override val webhookUrl = "https://59913958.ngrok.io"

--------------------------------------------------------------------------------------