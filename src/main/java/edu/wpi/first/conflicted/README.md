# Why is this here?

There are some classes in this repo that existed in 2020 WPILib and have been modified slightly for 2021. Unfortunately, if you already have 2020 WPILib imported then these classes will conflict and the classloader may load the "older" and then try to call methods that don't exist.