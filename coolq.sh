docker run -ti --rm --name cqhttp-ntt \
             -v /usr/local/ntt/coolq:/home/user/coolq \  # 将宿主目录挂载到容器内用于持久化 酷Q 的程序文件
             -p 9000:11452 \  # noVNC 端口，用于从浏览器控制 酷Q
             -p 5700:11212 \  # HTTP API 插件开放的端口
             -e COOLQ_ACCOUNT=198197419 \ # 要登录的 QQ 账号，可选但建议填
             -e CQHTTP_SERVE_DATA_FILES=yes \  # 允许通过 HTTP 接口访问 酷Q 数据文件
             richardchien/cqhttp:latest