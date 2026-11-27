
#pragma once

//This class abstracts the communication process between 2 layers

//hook it up to your send and receive functions and the protocol is handled by this class
class gwp{

    void (*send)();
    void (*receive)();


};