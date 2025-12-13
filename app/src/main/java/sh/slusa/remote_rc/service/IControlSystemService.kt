package sh.slusa.remote_rc.service


interface IControlSystemService {
    fun accelerate(enable: Boolean, value: Int)

    //fun backward(enable: Boolean)

    fun steer(value: Int)
//    fun steerLeft(enable: Boolean)
//
//    fun steerRight(enable: Boolean)

    fun emergencyStop()
}