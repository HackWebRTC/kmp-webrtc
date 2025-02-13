/*
 *  Copyright 2016 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

#import "ARDSettingsModel+Private.h"
#import "ARDSettingsStore.h"
#import <WebRTC/RTCCameraVideoCapturer.h>
#import <WebRTC/RTCMediaConstraints.h>
#import <WebRTC/RTCDefaultVideoEncoderFactory.h>

#include <stdlib.h>

NS_ASSUME_NONNULL_BEGIN

@interface ARDSettingsModel () {
    ARDSettingsStore* _settingsStore;
}
@end

@implementation ARDSettingsModel

- (NSArray<NSString*>*)availableVideoResolutions {
    NSMutableSet<NSArray<NSNumber*>*>* resolutions =
        [[NSMutableSet<NSArray<NSNumber*>*> alloc] init];
    for (AVCaptureDevice* device in [RTCCameraVideoCapturer captureDevices]) {
        for (AVCaptureDeviceFormat* format in
             [RTCCameraVideoCapturer supportedFormatsForDevice:device]) {
            CMVideoDimensions resolution =
                CMVideoFormatDescriptionGetDimensions(format.formatDescription);
            NSArray<NSNumber*>* resolutionObject =
                @[ @(resolution.width), @(resolution.height) ];
            [resolutions addObject:resolutionObject];
        }
    }

    NSArray<NSArray<NSNumber*>*>* sortedResolutions = [[resolutions allObjects]
        sortedArrayUsingComparator:^NSComparisonResult(
            NSArray<NSNumber*>* obj1, NSArray<NSNumber*>* obj2) {
            NSComparisonResult cmp =
                [obj1.firstObject compare:obj2.firstObject];
            if (cmp != NSOrderedSame) {
                return cmp;
            }
            return [obj1.lastObject compare:obj2.lastObject];
        }];

    NSMutableArray<NSString*>* resolutionStrings =
        [[NSMutableArray<NSString*> alloc] init];
    for (NSArray<NSNumber*>* resolution in sortedResolutions) {
        NSString* resolutionString =
            [NSString stringWithFormat:@"%@x%@", resolution.firstObject,
                                       resolution.lastObject];
        [resolutionStrings addObject:resolutionString];
    }

    return [resolutionStrings copy];
}

- (NSString*)currentVideoResolutionSettingFromStore {
    if ([[self settingsStore] videoResolution] == nil) {
        [[self settingsStore]
            setVideoResolution:[self defaultVideoResolutionSetting]];
    }
    return [[self settingsStore] videoResolution];
}

- (BOOL)storeVideoResolutionSetting:(NSString*)resolution {
    if (![[self availableVideoResolutions] containsObject:resolution]) {
        return NO;
    }
    [[self settingsStore] setVideoResolution:resolution];
    return YES;
}

- (NSArray<RTCVideoCodecInfo*>*)availableVideoCodecs {
    return [RTCDefaultVideoEncoderFactory supportedCodecs];
}

- (RTCVideoCodecInfo*)currentVideoCodecSettingFromStore {
    if ([[self settingsStore] videoCodec] == nil) {
        NSData* codecData = [NSKeyedArchiver
            archivedDataWithRootObject:[self defaultVideoCodecSetting]];
        //NSData* codecData = [NSKeyedArchiver archivedDataWithRootObject:[self defaultVideoCodecSetting] requiringSecureCoding:NO error:nil];
        [[self settingsStore] setVideoCodec:codecData];
    }
    NSData* codecData = [[self settingsStore] videoCodec];
    return [NSKeyedUnarchiver unarchiveObjectWithData:codecData];
    //return [NSKeyedUnarchiver unarchivedObjectOfClass:[RTCVideoCodecInfo class] fromData:codecData error:nil];
}

- (BOOL)storeVideoCodecSetting:(RTCVideoCodecInfo*)videoCodec {
    if (![[self availableVideoCodecs] containsObject:videoCodec]) {
        return NO;
    }
    NSData* codecData = [NSKeyedArchiver archivedDataWithRootObject:videoCodec];
    //NSData* codecData = [NSKeyedArchiver archivedDataWithRootObject:videoCodec requiringSecureCoding:NO error:nil];
    [[self settingsStore] setVideoCodec:codecData];
    return YES;
}

- (nullable NSNumber*)currentMaxBitrateSettingFromStore {
    if ([[self settingsStore] maxBitrate] == nil) {
        [[self settingsStore] setMaxBitrate:@(800)];
    }
    return [[self settingsStore] maxBitrate];
}

- (void)storeMaxBitrateSetting:(nullable NSNumber*)bitrate {
    [[self settingsStore] setMaxBitrate:bitrate];
}

#pragma mark - Testable

- (ARDSettingsStore*)settingsStore {
    if (!_settingsStore) {
        _settingsStore = [[ARDSettingsStore alloc] init];
    }
    return _settingsStore;
}

- (int)currentVideoResolutionWidthFromStore {
    NSString* resolution = [self currentVideoResolutionSettingFromStore];

    return [self videoResolutionComponentAtIndex:0 inString:resolution];
}

- (int)currentVideoResolutionHeightFromStore {
    NSString* resolution = [self currentVideoResolutionSettingFromStore];
    return [self videoResolutionComponentAtIndex:1 inString:resolution];
}

#pragma mark -

- (NSString*)defaultVideoResolutionSetting {
    return @"640x480";
}

- (RTCVideoCodecInfo*)defaultVideoCodecSetting {
    return [self availableVideoCodecs].firstObject;
}

- (int)videoResolutionComponentAtIndex:(int)index
                              inString:(NSString*)resolution {
    if (index != 0 && index != 1) {
        return 0;
    }
    NSArray<NSString*>* components =
        [resolution componentsSeparatedByString:@"x"];
    if (components.count != 2) {
        return 0;
    }
    return components[index].intValue;
}

@end
NS_ASSUME_NONNULL_END
