package sh.slusa.remote_rc.service


interface IControlSystemService {
    fun accelerate(enable: Boolean)

    fun backward(enable: Boolean)

    fun steerLeft(enable: Boolean)

    fun steerRight(enable: Boolean)

    fun emergencyStop()
}