https://github.com/bot4s/telegram
https://habr.com/ru/post/262247/
https://telegram.me/botfather
http://t.me/mtsPredBot
https://core.telegram.org/bots/api

Token in file application.conf

teleg {
  token = "xxx" --token of this telegram bot
  chatid = 322134338
  user_admin = 000001 --id of telegram user, who is admin
  proxy_host = "166.62.89.69" -- proxy host for telegram
  proxy_port = 64504 -- proxy port for telegram
}

cassandra {
 src {
  ip = "000.000.000.000"
 }
 dest {
  ip = "000.000.000.000"
 }
}

//cassandra driver configuration
datastax-java-driver {
  advanced.reconnect-on-init = true
  basic.request {
    timeout = 60 seconds
    consistency = LOCAL_ONE
    page-size = 5000
  }
  advanced.reconnection-policy {
    class = ExponentialReconnectionPolicy
    base-delay = 10 second
    max-delay = 60 seconds
  }
  advanced.heartbeat {
    interval = 30 seconds
    timeout = 60000 milliseconds
  }
  advanced.throttler {
    class = ConcurrencyLimitingRequestThrottler
    max-concurrent-requests = 100
    max-queue-size = 1000
  }
  advanced.connection {
    max-requests-per-connection = 1024
    pool {
      local.size = 1
      remote.size = 1
    }
  }
}