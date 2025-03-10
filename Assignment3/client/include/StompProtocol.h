#pragma once

#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include <unordered_map>
using std::string;

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    int subscriptionIdCounter_;
    int receiptIdCounter_;
    std::string username_;
    std::map<std::string, std::string> subscriptions;
    std::map<int, bool> receiptStatus;
    std::vector<Event> events;

public:

    StompProtocol();

    string createConnectFrame(string name , string password);

    string createSubscribeFrame(string destination);

    string createUnSubscribeFrame(string subscriptionId);

    string createSendFrame(const Event &event);

    string createDisconnectFrame(int receiptId);

    void setUserName(std::string username);

    std::string getUserName();
    
    void processLoginCommand(const std::string& command, ConnectionHandler*& connectionHandler , std::atomic<bool>& isRunning , std::atomic<bool>& isListenning , std::thread*& listenerThread);

    void processJoinCommand(const std::string& command, ConnectionHandler& connectionHandler);

    void processExitCommand(const std::string& command, ConnectionHandler& connectionHandler);

    void processReportCommand(const std::string& command, ConnectionHandler& connectionHandler);

    void processLogoutCommand(ConnectionHandler& connectionHandler);

    void processSummaryCommand(const std::string& command, ConnectionHandler& connectionHandler);
    
    void addEvent(const Event& event);
    
    void summarizeChannel(const std::string& channelName, const std::string& user, const std::string& fileName);

    void processMessageFromServer(string &message, ConnectionHandler*& connectionHandler, std::atomic<bool>& isRunning , std::atomic<bool>& isListenning , std::thread*& listenerThread);

    void handleMessage(std::istringstream& stream);

    void handleConnected(std::istringstream& stream, ConnectionHandler* connectionHandler);

    void handleReceipt(std::istringstream& stream, ConnectionHandler*& connectionHandler ,  std::atomic<bool>& isListenning , std::thread*& listenerThread);

    void handleError(std::istringstream& stream, ConnectionHandler* connectionHandler, std::atomic<bool>& isRunning);

    void shutdown(ConnectionHandler* connectionHandler, std::atomic<bool>& isRunning);

    void logout(ConnectionHandler*& connectionHandler , std::atomic<bool>& isListenning , std::thread*& listenerThread);

    void initializeListenerThread(std::thread*& listenerThread , ConnectionHandler*& connectionHandler , std::atomic<bool>& isRunning , std::atomic<bool>& isListenning);

};
