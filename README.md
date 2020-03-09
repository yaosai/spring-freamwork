# Spring5源码中文注释版
该项目为Spring5源码的中文注释版，Spring自带的README.md移至README_OLD.md

## 为什么会有这个项目
由于本人英语水平较差，不方便阅读。为了能更好的了解Spring的运行原理，通过断点调试了解Spring都做了什么

## 编译项目
本人采用IDEA2019.3.3+JDK1.8.0_241+Gradle-4.10.2编译此项目，具体过程可参考此篇博客
[https://blog.csdn.net/Dcwjh/article/details/104471560](https://blog.csdn.net/Dcwjh/article/details/104471560)

## 运行项目
编译完成可通过MySpringTests类启动Spring,在源码中打断点可了解项目的完整流程，文件地址
spring-context/src/test/java/org/springframework/context/annotation/MySpringTests.java