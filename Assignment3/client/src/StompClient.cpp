#include <iostream>
#include <thread>
#include <atomic>
#include <mutex>
#include <string>
#include "ConnectionHandler.h"
#include "StompProtocol.h"
#include "event.h"

std::mutex connectionMutex; // Mutex for thread-safe access to ConnectionHandler

int main() {
    StompProtocol protocol;
    ConnectionHandler* connectionHandler = nullptr;
    std::atomic<bool> isRunning(true);
    std::atomic<bool> isListenning(false);
    std::thread* listenerThread = nullptr;

    std::cout << "Welcome to the STOMP Client! Type commands below:" << std::endl;

    // Main thread for handling input
    while (isRunning) {
        
        std::string userInput;
        std::getline(std::cin, userInput);

        // Extract the command (first word)
        std::string command = userInput.substr(0, userInput.find(' '));

        if (command == "login") {
            if (connectionHandler != nullptr) {
                std::cerr << "The client is already logged in, log out before trying again" << std::endl;
                continue;
            }
            protocol.processLoginCommand(userInput, connectionHandler , isRunning , isListenning , listenerThread);

        } else if (command == "join") {
            if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                protocol.processJoinCommand(userInput, *connectionHandler);
            } else {
                std::cerr << "Please login first." << std::endl;
            }
        } else if (command == "report") {
            if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                protocol.processReportCommand(userInput, *connectionHandler);
            } else {
                std::cerr << "Please login first." << std::endl;
            }
        } else if (command == "logout") {
            if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                protocol.processLogoutCommand(*connectionHandler);    

            } else {
                std::cerr << "Not logged in." << std::endl;
            }
        } else if (command == "exit") {
            if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                protocol.processExitCommand(userInput, *connectionHandler);
            } else {
                std::cerr << "Please login first." << std::endl;
            }
         } else if (command == "summary") {
            if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                protocol.processSummaryCommand(userInput, *connectionHandler);
            } else {
                std::cerr << "Please login first." << std::endl;
            }
        } else {
            std::cerr << "Unknown command: " << command << std::endl;
        }

        if (!isRunning) {
            break;
        }
    }

    // Wait for the listener thread to finish
    if (listenerThread->joinable()) {
        listenerThread->join();
        delete listenerThread;
    }
    delete connectionHandler;
    std::cout << "Client terminated gracefully." << std::endl;
    return 0;
}
