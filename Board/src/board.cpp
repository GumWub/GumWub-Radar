    
    #include "board.h"
    #include"motor_steps.h"

    //Constructor
    Board::Board():
    m_esp(),
    m_radar(),
    m_motor(ScanMode::fast)
    {
    }
    
    void Board::Init()
    {
        m_esp.Init();
        m_radar.Init(coverage, timeout);
        m_motor.Init();
    }


    void Board::SendScanReport(){}
    void Board::SendData(){}
    void Board::ReceiveData(){}

    void Board::HandleRoutine()
    {
        float scan = m_radar.Scan();
        m_motor.NextStep();
    }