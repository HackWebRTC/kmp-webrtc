#include "loopback.h"

#include <atomic>
#include <signal.h>
#include <stdio.h>
#include <unistd.h>

std::atomic<bool> running(true);

void signal_handler(int sig) {
    running = false;
}

int main(int argc, char *argv[]) {
    if (argc != 2) {
        printf("Usage: loopback <input video file path>\n");
        return -1;
    }

    signal(SIGINT, signal_handler);
    loopback(argv[1]);
    while (running) {
        sleep(100);
    }
    return 0;
}
