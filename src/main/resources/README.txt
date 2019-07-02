
--------------------------------------------------------------------------------------
KEYS:
C:\jdk1.8.0_181\bin\keytool -genkey -keyalg RSA -alias mtspredbot -keystore mtspredbot.jks -storepass 333333 -validity 360 -keysize 2048
C:\jdk1.8.0_181\bin\keytool -importkeystore -srckeystore mtspredbot.jks -destkeystore mtspredbot.p12 -srcstoretype jks -deststoretype pkcs12
C:\openssl\openssl pkcs12 -in mtspredbot.p12 -out mtspredbot.pem

manually edit mtspredbot.pem and stay only public cert:

Bag Attributes
    friendlyName: mtspredbot
    localKeyID:
...
-----BEGIN CERTIFICATE-----
hexstaffhere
...
-----END CERTIFICATE-----

--------------------------------------------------------------------------------------
control.conf

teleg {
  token="xxxxxxxxxxx"
  chatid=111
  user_admin=222
  webhook_port=8443
  webhookUrl="https://12.34.56.78:8443"
  keyStorePassword=333333
}

cassandra {
  src {
    ip="12.34.56.78"
    dc="datacenter1"
  }
}

--------------------------------------------------------------------------------------