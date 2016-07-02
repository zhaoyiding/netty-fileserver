# netty-fileserver
基于Netty的文件服务器

    NettyFileServer类里有root这个静态变量，表示 被请求文件 的存放目录。修改为你想要存放的目录。
    main方法里可以修改绑定的端口号，默认为8080。
    
    运行NettyFileServer类，输入如： http://localhost:8080/xxxx.pdf，浏览器弹出相应的pdf文件下载框。
