//
//  main.m
//  macApp
//
//  Created by linker on 2025/2/24.
//

#import <Cocoa/Cocoa.h>
#import "APPRTCAppDelegate.h"

int main(int argc, const char * argv[]) {
    @autoreleasepool {
        [NSApplication sharedApplication];
        APPRTCAppDelegate* delegate = [[APPRTCAppDelegate alloc] init];
        [NSApp setDelegate:delegate];
        [NSApp run];
    }
    return 0;
}
