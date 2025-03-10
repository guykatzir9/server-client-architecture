#include "StompProtocol.h"
#include <fstream>
#include <ctime>
#include <sstream>
#include <iomanip>


using std::string;

StompProtocol::StompProtocol() : subscriptionIdCounter_(0) ,
receiptIdCounter_(0){}

std::string StompProtocol::getUserName(){
      return username_;
}

void StompProtocol::setUserName(std::string username){
      username_ = username;
}

std::string epochToDate(int epoch) {
    // Convert epoch time to time_t
    std::time_t time = epoch;

    // Convert to a tm structure
    std::tm* tm = std::localtime(&time);

    // Format the date and time into a string
    std::ostringstream oss;
    oss << std::put_time(tm, "%d/%m/%y %H:%M"); // Format: DD/MM/YY HH:MM
    return oss.str();
}


std::string StompProtocol::createConnectFrame(string name, string password) {

    std::ostringstream frame;
    frame << "CONNECT\n"
          << "accept-version:1.2\n"
          << "host:stomp.cs.bgu.ac.il\n"
          << "login:" << name << "\n"
          << "passcode:" << password << "\n"
          << "\n\0";
    return frame.str();
}

std::string StompProtocol::createSubscribeFrame(string destination)
{
    std::ostringstream frame;
    frame << "SUBSCRIBE\n"
          << "destination:" << destination << "\n"
          << "id:" << subscriptionIdCounter_ <<"\n"
          << "receipt:" << receiptIdCounter_ << "\n"
          << "\n\0";
    receiptIdCounter_++;
    return frame.str();
}

//note: here we will use the subscriptionId and not the subscriptionId counter.
string StompProtocol::createUnSubscribeFrame(string subscriptionId) {

    std::ostringstream frame;
    frame << "UNSUBSCRIBE\n"
          << "id:" << subscriptionId <<"\n"
          << "receipt:" << receiptIdCounter_ << "\n"
          << "\n\0";

    receiptIdCounter_++;
    return frame.str();
}

string StompProtocol::createSendFrame(const Event &event) {

    std::ostringstream frame;
    frame << "SEND\n"
          << "destination:" << event.get_channel_name() <<"\n\n"
          << "user:" << event.getEventOwnerUser() << "\n"
          << "city:" << event.get_city() << "\n"
          << "event name:" << event.get_name() << "\n"
          << "date time:" << event.get_date_time() << "\n"
          << "general information:\n";
          //adding the general information storedd in a map in the event  
          for (const auto &entry : event.get_general_information()) {
                frame << "\t" << entry.first << ":" << entry.second << "\n";
          }
    frame << "description:\n";
    frame << event.get_description() << "\n";
    frame << "\0";
    return frame.str();
}

string StompProtocol::createDisconnectFrame(int recepitId){ 

      std::ostringstream frame;
      frame << "DISCONNECT\n"
            << "receipt:" << recepitId << "\n"
            << "\n\0";
            return frame.str();
}

/*
-----------------------------------------

      Processing inputs from user

-----------------------------------------
*/

void StompProtocol::processLoginCommand(const std::string& command, ConnectionHandler*& connectionHandler , std::atomic<bool>& isRunning , std::atomic<bool>& isListenning , std::thread*& listenerThread) {
    // Split the command into arguments
    std::istringstream iss(command);
    std::vector<std::string> args(std::istream_iterator<std::string>{iss}, std::istream_iterator<std::string>());

    // Validate the number of arguments and command syntax
    if (args.size() != 4 || args[0] != "login") {
        std::cerr << "Login command requires 3 arguments: login {host:port} {username} {password}" << std::endl;
        return;
    }
    
    std::string hostAndPort = args[1];
    std::string username = args[2];
    std::string password = args[3];

    // Parse host and port
    size_t colonPos = hostAndPort.find(":");
    if (colonPos == std::string::npos) {
        std::cerr << "Invalid host:port format. Expected format is host:port." << std::endl;
        return;
    }

    std::string host = hostAndPort.substr(0, colonPos);
    short port = std::stoi(hostAndPort.substr(colonPos + 1));

    // Initialize the ConnectionHandler (passed as a parameter)
    connectionHandler = new ConnectionHandler(host, port);

    // Attempt to connect to the server
    if (!connectionHandler->connect()) {
        std::cerr << "Could not connect to server." << std::endl;
        return;
    }
    isListenning.store(true);
    this->initializeListenerThread(listenerThread , connectionHandler , isRunning , isListenning);    
    
    
    // Create the CONNECT frame using the current StompProtocol instance
    std::string connectFrame = this->createConnectFrame(username, password);
    std::cout << "done creating the connect frame" << std::endl;
    std::cout << connectFrame << std::endl;
    

    // Send the CONNECT frame to the server
    if (!connectionHandler->sendFrameAscii(connectFrame , '\0')) {
        std::cerr << "Failed to send CONNECT frame." << std::endl;
        delete connectionHandler;
        connectionHandler = nullptr;
        return;
    }

    connectionHandler->setIsConnected(true);
    setUserName(username);
    std::cout << "done sending the connect frame" << std::endl;
}


void StompProtocol::processJoinCommand(const std::string& command, ConnectionHandler& connectionHandler) {
    // Combined check for connectionHandler being null or not connected
    if (!connectionHandler.isConnected()) {
        std::cerr << "Please log in first." << std::endl;
        return;
    }

    // Split the command into arguments
    std::istringstream iss(command);
    std::vector<std::string> args(std::istream_iterator<std::string>{iss}, std::istream_iterator<std::string>());

    // Validate the command
    if (args.size() != 2 || args[0] != "join") {
        std::cerr << "Join command requires 1 argument: join {channel_name}" << std::endl;
        return;
    }

    // Extract the channel name
    std::string channelName = args[1];

    // Create the SUBSCRIBE frame
    std::string subscribeFrame = createSubscribeFrame(channelName);
    // Add to the subscriptions map
    subscriptions[channelName] = std::to_string(subscriptionIdCounter_);
    
    subscriptionIdCounter_++;

    // Send the SUBSCRIBE frame using ConnectionHandler
    if (!connectionHandler.sendFrameAscii(subscribeFrame , '\0')) {
        std::cerr << "Failed to send SUBSCRIBE frame for channel: " << channelName << std::endl;
        return;
    }
     
    // Output success message
    std::cout << "SUBSCRIBE frame sent for channel: " << channelName << std::endl;

}

void StompProtocol::processExitCommand(const std::string& command, ConnectionHandler& connectionHandler) {
    // Split the command into arguments
    std::istringstream iss(command);
    std::vector<std::string> args(std::istream_iterator<std::string>{iss}, std::istream_iterator<std::string>());

    // Validate the command format
    if (args.size() != 2 || args[0] != "exit") {
        std::cerr << "Exit command requires 1 argument: exit {channel_name}" << std::endl;
        return;
    }

    // Extract the channel name
    std::string channelName = args[1];

    // Check if the channel is in the subscription map
    if (subscriptions.find(channelName) == subscriptions.end()) {
        std::cerr << "You are not subscribed to channel: " << channelName << std::endl;
        return;
    }

    // Get the subscription ID for the channel
    std::string subscriptionId = subscriptions[channelName];

    // Generate the UNSUBSCRIBE frame
    std::string unsubscribeFrame = createUnSubscribeFrame(subscriptionId);

    // Send the UNSUBSCRIBE frame using ConnectionHandler
    if (!connectionHandler.sendFrameAscii(unsubscribeFrame , '\0')) {
        std::cerr << "Failed to send UNSUBSCRIBE frame for channel: " << channelName << std::endl;
        return;
    }

    // Remove the channel from the subscription map
    subscriptions.erase(channelName);

    // Output success message
    std::cout << "Unsubscribed from channel: " << channelName << std::endl;
}

//if we decide to hold username in connection handler, remove username parameter.
void StompProtocol::processReportCommand(const std::string& command, ConnectionHandler& connectionHandler) {
    // Split the command into arguments
    std::istringstream iss(command);
    std::vector<std::string> args(std::istream_iterator<std::string>{iss}, std::istream_iterator<std::string>());

    // Validate the command format
    if (args.size() != 2 || args[0] != "report") {
        std::cerr << "Report command requires 1 argument: report {file}" << std::endl;
        return;
    }

    // Extract the file name
    std::string fileName = args[1];

    // Parse the events file
    names_and_events parsedData;
    try {
        parsedData = parseEventsFile(fileName); // Parse the JSON file into events and channel name
    } catch (const std::exception& e) {
        std::cerr << "Failed to parse events file: " << e.what() << std::endl;
        return;
    }

    // Extract channel name and events
    std::string channelName = parsedData.channel_name;
    const std::vector<Event>& events = parsedData.events;

    // Iterate over each event and send a SEND frame
    for (Event event : events) {
        // Set the event owner user
        event.setEventOwnerUser(username_);

        // Create the SEND frame using createSendFrame
        std::string sendFrame = createSendFrame(event);

        // Send the SEND frame using ConnectionHandler
        if (!connectionHandler.sendFrameAscii(sendFrame, '\0')) {
            std::cerr << "Failed to send SEND frame for event: " << event.get_name() << std::endl;
            return;
        }

        // Output success for the current event
        std::cout << "Event reported: " << event.get_name() << " to channel: " << channelName << std::endl;

        addEvent(event);
    }
}


void StompProtocol::processLogoutCommand(ConnectionHandler& connectionHandler) {
    // Create the DISCONNECT frame
    std::string disconnectFrame = createDisconnectFrame(receiptIdCounter_);
    
    // Send the DISCONNECT frame using the ConnectionHandler
    if (!connectionHandler.sendFrameAscii(disconnectFrame , '\0')) {
        std::cerr << "Failed to send DISCONNECT frame." << std::endl;
        return;
    }
    receiptStatus[receiptIdCounter_] = false;
    receiptIdCounter_++;

    // Confirmation of sending the frame
    std::cout << "DISCONNECT frame sent successfully." << std::endl;
}

void StompProtocol::processSummaryCommand(const std::string& command, ConnectionHandler& connectionHandler){
    std::istringstream stream(command);
    std::string token;

    // Parse the command (skip the first token "summary")
    stream >> token; // This should be "summary"
    if (token != "summary") {
        std::cerr << "Invalid command format: Expected 'summary' at the start." << std::endl;
        return;
    }
    // Extract channel_name, user, and file
    std::string channelName, user, file;
    stream >> channelName >> user >> file;
    summarizeChannel(channelName, user, file);


}

/*
---------------------------------------------------

      Handeling Incoming frames from server

---------------------------------------------------
*/

void StompProtocol::addEvent(const Event& event) {
    events.push_back(event);

    // Sort the events by date_time, then by name
    std::sort(events.begin(), events.end(), [](const Event& a, const Event& b) {
        if (a.get_date_time() != b.get_date_time()) {
            return a.get_date_time() < b.get_date_time();
        }
        return a.get_name() < b.get_name();
    });
}

void StompProtocol::summarizeChannel(const std::string& channelName, const std::string& user, const std::string& fileName) {
    std::vector<Event> filteredEvents;

    // Filter events by channel and user
    for (const auto& event : events) {
        if (event.get_channel_name() == channelName && event.getEventOwnerUser() == user) {
            filteredEvents.push_back(event);
        }
    }

    // Check if filteredEvents is empty
    if (filteredEvents.empty()) {
        std::cout << "No events found for channel: " << channelName << " and user: " << user << std::endl;
        return;
    }

    // Generate statistics
    int totalReports = filteredEvents.size();
    int activeCount = 0;
    int forcesArrivalCount = 0;
    for (const auto& event : filteredEvents) {
        const auto& generalInfo = event.get_general_information();
        if (generalInfo.find("active") != generalInfo.end() && generalInfo.at("active") == "true") {
            activeCount++;
        }
        if (generalInfo.find("forces_arrival_at_scene") != generalInfo.end() && generalInfo.at("forces_arrival_at_scene") == "true") {
            forcesArrivalCount++;
        }
    }

    // Open the file
    std::ofstream file(fileName, std::ios::out | std::ios::trunc);
    if (!file.is_open()) {
        std::cerr << "Failed to open file: " << fileName << std::endl;
        return;
    }

    // Write the summary
    file << "Channel: " << channelName << "\n";
    file << "Stats:\n";
    file << "Total: " << totalReports << "\n";
    file << "Active: " << activeCount << "\n";
    file << "Forces arrival at scene: " << forcesArrivalCount << "\n";
    file << "Event Reports:\n";

    // Write each filtered event
    for (size_t i = 0; i < filteredEvents.size(); ++i) {
        const auto& event = filteredEvents[i];
        file << "Report_" << i + 1 << ":\n";
        file << "city: " << event.get_city() << "\n";
        file << "date time: " << epochToDate(event.get_date_time()) << "\n";
        file << "event name: " << event.get_name() << "\n";

        // Generate the summary
        std::string description = event.get_description();
        if (!description.empty()) {
            std::string summary = description.substr(0, 27);
            if (description.length() > 27) {
                summary += "...";
            }
            file << "summary: " << summary << "\n";
        } else {
            file << "summary: (No description available)\n";
        }
    }

    file.close();
    std::cout << "Summary written to " << fileName << std::endl;
}



void StompProtocol::processMessageFromServer(string &message, ConnectionHandler*& connectionHandler, std::atomic<bool>& isRunning ,std::atomic<bool>& isListenning , std::thread*& listenerThread) {
    // Parse the message type (first line of the frame)
    std::istringstream stream(message);
    std::string line;
    std::getline(stream, line); // Get the first line of the frame (e.g., MESSAGE, RECEIPT, ERROR)

    if (line == "MESSAGE") {
        handleMessage(stream);
    } else if (line == "RECEIPT") {
        handleReceipt(stream, connectionHandler , isListenning , listenerThread);
    } else if (line == "ERROR") {
        handleError(stream, connectionHandler, isRunning);
    } else if (line == "CONNECTED") {
        handleConnected(stream , connectionHandler);
    }   
     else {
        std::cout << line << std::endl;
        std::cerr << "Unknown message type from server: " << line << std::endl;
    }
}

void StompProtocol::handleConnected(std::istringstream& stream, ConnectionHandler* connectionHandler){
           
    std::cout << "Login successful" << std::endl;
    connectionHandler->setIsConnected(true); // Update connection status
}



void StompProtocol::handleMessage(std::istringstream& stream) {
    // Extract the destination header first
    std::string line;
    std::string destination;

    while (std::getline(stream, line) && !line.empty()) {
        size_t delimiterPos = line.find(':');
        if (delimiterPos != std::string::npos) {
            std::string key = line.substr(0, delimiterPos);
            std::string value = line.substr(delimiterPos + 1);

            if (key == "destination") {
                destination = value;
                break; // Stop after extracting destination
            }
        }
    }

    // Extract the full message body as a single string
    std::string frameBody((std::istreambuf_iterator<char>(stream)), std::istreambuf_iterator<char>());

    // Use the Event constructor to parse the frame body
    Event event(frameBody);

    // Map destination to channel_name explicitly
    if (!destination.empty()) {
        event.set_channel_name(destination);
    }

    // Add the event to the list of events
    if(event.getEventOwnerUser() != username_){
        addEvent(event);
    }
    
}

void StompProtocol::handleReceipt(std::istringstream& stream, ConnectionHandler*& connectionHandler , std::atomic<bool>& isListenning , std::thread*& listenerThread) {
    std::string header;
    std::string receiptId;

    // Parse headers to extract the receipt-id
    while (std::getline(stream, header) && !header.empty()) {
        size_t delimiterPos = header.find(':');
        if (delimiterPos != std::string::npos) {
            std::string key = header.substr(0, delimiterPos);
            std::string value = header.substr(delimiterPos + 1);

            // Remove leading/trailing whitespace
            key.erase(0, key.find_first_not_of(" \t"));
            key.erase(key.find_last_not_of(" \t") + 1);
            value.erase(0, value.find_first_not_of(" \t"));
            value.erase(value.find_last_not_of(" \t") + 1);

            // Check if this is the receipt-id header
            if (key == "receipt-id") {
                receiptId = value;
                break;
            }
        }
    }

    if (receiptId.empty()) {
        std::cerr << "Error: Received RECEIPT frame without a receipt-id." << std::endl;
        return;
    }
    auto it = receiptStatus.find(std::stoi(receiptId)); // Convert receipt-id to integer
    if (it != receiptStatus.end()) {
        // Mark the receipt as acknowledged
        it->second = true;
        std::cout << "Receipt acknowledged: Disconnect receipt" << std::endl;
        logout(connectionHandler , isListenning , listenerThread);
      }
}

void StompProtocol::handleError(std::istringstream& stream, ConnectionHandler* connectionHandler, std::atomic<bool>& isRunning) {

      std::cout << stream.str() << std::endl;
      shutdown(connectionHandler, isRunning);

}

void StompProtocol::logout(ConnectionHandler*& connectionHandler , std::atomic<bool>& isListenning , std::thread*& listenerThread){
      std::cout << "Logging out..." << std::endl;
      receiptIdCounter_ = 0;
      subscriptionIdCounter_ = 0;
      username_.clear();
      subscriptions.clear();
      receiptStatus.clear();
      events.clear();
      connectionHandler->close();
      //connectionHandler.setIsConnected(false);
      delete connectionHandler;
      connectionHandler = nullptr;
      isListenning.store(false);
      listenerThread = nullptr;
}

void StompProtocol::shutdown(ConnectionHandler* connectionHandler, std::atomic<bool>& isRunning){
      std::cout << "Shutting down..." << std::endl;
      connectionHandler->close();
      connectionHandler->setIsConnected(false);
      isRunning.store(false);
}

void StompProtocol::initializeListenerThread(std::thread*& listenerThread , ConnectionHandler*& connectionHandler , std::atomic<bool>& isRunning , std::atomic<bool>& isListenning) { 


                    listenerThread = new std::thread ([&]() {
                    while (isListenning) {
                        if (connectionHandler != nullptr && connectionHandler->isConnected()) {
                            std::string fullMessage;
                            if (!connectionHandler->getFrameAscii(fullMessage, '\0')) { // Use getFrameAscii to read the full frame
                                std::cerr << "Error reading from server. Disconnecting..." << std::endl;
                                isListenning = false;
                                break;
                            }       
                            else {
                                this->processMessageFromServer(fullMessage, connectionHandler, isRunning , isListenning ,  listenerThread); // Pass to the protocol for processing
            	            }

        	            }
    	            }
                });

}


