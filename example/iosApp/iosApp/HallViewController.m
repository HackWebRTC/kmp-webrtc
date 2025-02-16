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

@import SDAutoLayout;
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
    
    UIButton* create = [UIButton buttonWithType:UIButtonTypeSystem];
    [create setTitle:@"loopback" forState:UIControlStateNormal];
    create.titleLabel.font = [UIFont systemFontOfSize:20];
    [self.view addSubview:create];
    create.sd_layout.widthIs(180)
        .heightIs(20)
        .topSpaceToView(self.view, 30)
        .centerXEqualToView(self.view);
    
    [create addTarget:self
               action:@selector(onLoopback:)
     forControlEvents:UIControlEventTouchUpInside];

    UITableViewCell* audioOnlyCell =
        [[UITableViewCell alloc] initWithFrame:CGRectZero];
    audioOnlyCell.selectionStyle = UITableViewCellSelectionStyleNone;
    _audioOnlySwitch = [[UISwitch alloc] initWithFrame:CGRectZero];
    audioOnlyCell.accessoryView = _audioOnlySwitch;
    audioOnlyCell.textLabel.text = @"Audio only";
    [self.view addSubview:audioOnlyCell];
    audioOnlyCell.sd_layout.widthIs(180)
        .heightRatioToView(create, 1)
        .topSpaceToView(create, 20)
        .centerXEqualToView(self.view);

    UIButton* shareLog = [UIButton buttonWithType:UIButtonTypeSystem];
    [shareLog setTitle:@"share log" forState:UIControlStateNormal];
    shareLog.titleLabel.font = [UIFont systemFontOfSize:20];
    [self.view addSubview:shareLog];
    shareLog.sd_layout.widthIs(180)
        .heightRatioToView(create, 1)
        .topSpaceToView(audioOnlyCell, 20)
        .centerXEqualToView(self.view);
    
    [shareLog addTarget:self
                 action:@selector(onShareLog:)
       forControlEvents:UIControlEventTouchUpInside];

    UILabel* version = [[UILabel alloc] initWithFrame:CGRectZero];
    version.text = [NSString stringWithFormat:@"kmp-webrtc %@", [CFPeerConnectionClient versionName]];
    version.textAlignment = NSTextAlignmentCenter;
    [self.view addSubview:version];
    version.sd_layout.widthIs(250)
        .heightRatioToView(create, 1)
        .topSpaceToView(shareLog, 10)
        .centerXEqualToView(self.view);
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
