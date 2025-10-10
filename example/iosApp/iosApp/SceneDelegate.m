#import "SceneDelegate.h"
#import "HallViewController.h"

@implementation SceneDelegate

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions {
    // 使用此方法选择性地配置并将给定的场景附加到UIWindow
    if ([scene isKindOfClass:[UIWindowScene class]]) {
        UIWindowScene *windowScene = (UIWindowScene *)scene;
        self.window = [[UIWindow alloc] initWithWindowScene:windowScene];
        self.window.frame = windowScene.coordinateSpace.bounds;
        
        // 创建你的根视图控制器
        HallViewController *rootViewController = [[HallViewController alloc] init];
        self.window.rootViewController = rootViewController;
        
        [self.window makeKeyAndVisible];
    }
}

- (void)sceneDidDisconnect:(UIScene *)scene {
    // 当场景被释放时调用
}

- (void)sceneDidBecomeActive:(UIScene *)scene {
    // 当场景从非活动状态转换为活动状态时调用
}

- (void)sceneWillResignActive:(UIScene *)scene {
    // 当场景从活动状态转换为非活动状态时调用
}

- (void)sceneWillEnterForeground:(UIScene *)scene {
    // 当场景从后台进入前台时调用
}

- (void)sceneDidEnterBackground:(UIScene *)scene {
    // 当场景从前台进入后台时调用
}

@end
