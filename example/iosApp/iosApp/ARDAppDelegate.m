/*
 *  Copyright 2013 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import "ARDAppDelegate.h"

#import "HallViewController.h"

@implementation ARDAppDelegate {
  UIWindow *_window;
}

#pragma mark - UIApplicationDelegate methods

- (BOOL)application:(UIApplication *)application
    didFinishLaunchingWithOptions:(NSDictionary *)launchOptions {

  _window =  [[UIWindow alloc] initWithFrame:[[UIScreen mainScreen] bounds]];
  [_window makeKeyAndVisible];
  HallViewController *viewController = [[HallViewController alloc] init];

  UINavigationController *root =
      [[UINavigationController alloc] initWithRootViewController:viewController];
  root.navigationBar.translucent = NO;
  _window.rootViewController = root;

  return YES;
}

@end
