    
    #include "board.h"

    //Constructor
    Board::Board():
    m_esp(),
    m_radar(),
    m_motor()
    {
    }
    
    void Board::Init()
    {
        pinMode(2, INPUT);
    }
    void Board::SendScanReport(){}
    void Board::RotateMotor(){}
    void Board::Scan(){}
    void Board::SendData(){}
    void Board::ReceiveData(){}

    void Board::HandleRoutine(){}