# OpDrop - DDOS Android application 
## Слава Укрaїні! - Героям слава! 🇺🇦

<img src="https://github.com/ohnu11/OpDrop/blob/main/play.gif" width="300" height="600" />

A lot of people want to help Ukraine in this war but they don't have enough knowledge to use some scripts but all of us have phones. Atack list will be updated from the official telegram channel [IT ARMY of Ukraine](https://t.me/itarmyofukraine2022).

Just download the APK and launch it! -> [LATEST RELEASE](https://github.com/ohnu11/OpDrop/releases/tag/Release)

**Note:** this app is not about clean code / nice architecture etc. I don't need your comment about it. If you want to improve something - you are welcome! Just push your changes! If you have ideas, feel free to open issues/discussions here.

Here is a config example:
```
{
    "version": 1,
    "max_threads": 200,
    "urls": [
        {"url": "udp://123.45.67.89:53", "method":"UDP" },
        {"url": "tcp://123.45.67.89:53", "method":"TCP" },
        {"url": "https://wow-super-website.com", "method":"GET" },
        {"url": "https://wow-super-website.com?search=hehe", "method":"GET" },
        {"url": "https://wow-super-website.com/login.php", "method":"POST", "data": "username=aaa&password=aaa1234" }
    ],
    "settings":{
        "latest_code_version": 1,
        "update_dialog_text": "New version is ready! Please, update the app -> <a href='PUT NEW APK LINK HERE'>PUT NEW APK LINK HERE</a>"
    }
}
```

### Caution:
- I wrote this app for educational not for destructive purposes and illegal actions, so I won't be responsible for that
- Please Don't Attack websites without the owners consent
- ...bla-bla :)

### TO-DOs:
- [x] Working prototype
- [ ] Internal VPN or Proxy. Auto-update feature.
- [ ] Improve TCP/UDP approach. UDP eats a lot of traffic.
- [ ] Advanced mode (change threads/intervals/timeouts manually).
- [ ] Improve UI
- [ ] ...
