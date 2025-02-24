//
//  HallViewController.m
//  iOSExample
//
//  Created by Piasy on 2019/7/4.
//

#import "HallViewController.h"
#import "ARDSettingsModel.h"
#import "ARDSettingsViewController.h"
#import "CallViewController.h"
#import "ARDToast.h"

@import WebRTC;

static NSString* const barButtonImageString = @"ic_settings_black_24dp.png";

@implementation HallViewController {
    UISwitch* _audioOnlySwitch;
}

- (void)viewDidLoad {
    [super viewDidLoad];

    self.title = @"kmp-webrtc";
    self.view.backgroundColor = [UIColor whiteColor];
    [self addSettingsBarButton];

    UIButton* loopback = [UIButton buttonWithType:UIButtonTypeSystem];
    [loopback setTitle:@"loopback" forState:UIControlStateNormal];
    [loopback addTarget:self
                 action:@selector(onLoopback:)
       forControlEvents:UIControlEventTouchUpInside];
    loopback.titleLabel.font = [UIFont systemFontOfSize:20];

    UIStackView *switchStackView = [[UIStackView alloc] init];
    switchStackView.axis = UILayoutConstraintAxisHorizontal;
    switchStackView.distribution = UIStackViewDistributionFillProportionally;
    switchStackView.spacing = 10;
    UILabel *switchLabel = [[UILabel alloc] init];
    switchLabel.text = @"Audio only";
    switchLabel.font = [UIFont systemFontOfSize:20];
    [switchStackView addArrangedSubview:switchLabel];
    _audioOnlySwitch = [[UISwitch alloc] init];
    [switchStackView addArrangedSubview:_audioOnlySwitch];

    UIButton* shareLog = [UIButton buttonWithType:UIButtonTypeSystem];
    [shareLog setTitle:@"share log" forState:UIControlStateNormal];
    [shareLog addTarget:self
                 action:@selector(onShareLog:)
       forControlEvents:UIControlEventTouchUpInside];
    shareLog.titleLabel.font = [UIFont systemFontOfSize:20];

    UILabel* version = [[UILabel alloc] initWithFrame:CGRectZero];
    version.text = [NSString stringWithFormat:@"kmp-webrtc %@", [CFPeerConnectionClient versionName]];
    version.font = [UIFont systemFontOfSize:20];

    UIStackView *stackView = [[UIStackView alloc] init];
    stackView.axis = UILayoutConstraintAxisVertical;
    stackView.alignment = UIStackViewAlignmentLeading;
    stackView.distribution = UIStackViewDistributionFillEqually;
    stackView.spacing = 10;

    [stackView addArrangedSubview:loopback];
    [stackView addArrangedSubview:switchStackView];
    [stackView addArrangedSubview:shareLog];
    [stackView addArrangedSubview:version];
    [self.view addSubview:stackView];

    // 使用 Auto Layout 约束 StackView 的位置和大小
    stackView.translatesAutoresizingMaskIntoConstraints = NO;
    // 水平居中
    NSLayoutConstraint *centerXConstraint =
        [NSLayoutConstraint constraintWithItem:stackView
                                     attribute:NSLayoutAttributeCenterX
                                     relatedBy:NSLayoutRelationEqual
                                        toItem:self.view
                                     attribute:NSLayoutAttributeCenterX
                                    multiplier:1.0
                                      constant:0];
    // 距离顶部 50
    NSLayoutConstraint *topConstraint =
        [NSLayoutConstraint constraintWithItem:stackView
                                     attribute:NSLayoutAttributeTop
                                     relatedBy:NSLayoutRelationEqual
                                        toItem:self.view.safeAreaLayoutGuide
                                     attribute:NSLayoutAttributeTop
                                    multiplier:1.0
                                      constant:50];
    // 激活约束
    [NSLayoutConstraint activateConstraints:@[centerXConstraint, topConstraint]];
}

- (void)viewDidAppear:(BOOL)animated {
}

- (void)addSettingsBarButton {
    UIBarButtonItem* settingsButton = [[UIBarButtonItem alloc]
        initWithImage:[UIImage imageNamed:barButtonImageString]
                style:UIBarButtonItemStylePlain
               target:self
               action:@selector(showSettings:)];
    self.navigationItem.rightBarButtonItem = settingsButton;
}

- (void)showSettings:(id)sender {
    ARDSettingsViewController* settingsController =
        [[ARDSettingsViewController alloc]
            initWithStyle:UITableViewStyleGrouped
            settingsModel:[[ARDSettingsModel alloc] init]];

    UINavigationController* navigationController =
        [[UINavigationController alloc]
            initWithRootViewController:settingsController];
    [self presentViewController:navigationController
                       animated:YES
                     completion:nil];
}

- (void)onShareLog:(id)sender {
    NSArray* paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory,
                                                         NSUserDomainMask, YES);
    NSString* docDir = [paths objectAtIndex:0];
    NSString* logDir = [docDir stringByAppendingPathComponent:@"log"];
    NSArray* files =
        [[NSFileManager defaultManager] subpathsOfDirectoryAtPath:logDir
                                                            error:nil];
    NSMutableArray* logs = [[NSMutableArray alloc] init];
    for (NSString* file in files) {
        if ([file hasSuffix:@".xlog"]) {
            [logs addObject:[NSURL fileURLWithPath:[NSString stringWithFormat:@"%@/%@", logDir, file]]];
        }
    }
    if (logs.count > 0) {
        UIActivityViewController* activityVC =
            [[UIActivityViewController alloc] initWithActivityItems:logs
                                              applicationActivities:nil];
        activityVC.excludedActivityTypes = @[
            UIActivityTypePrint, UIActivityTypeCopyToPasteboard,
            UIActivityTypeAssignToContact, UIActivityTypeSaveToCameraRoll
        ];
        [self presentViewController:activityVC animated:YES completion:nil];
    }
}

- (void)onLoopback:(id)sender {
    CallViewController* viewController = [[CallViewController alloc] initWithAudioOnly:_audioOnlySwitch.isOn isLandscape:false];
    viewController.modalTransitionStyle = UIModalTransitionStyleCrossDissolve;
    [self presentViewController:viewController animated:YES completion:nil];
    [[UIApplication sharedApplication] setIdleTimerDisabled:YES];
}

@end
