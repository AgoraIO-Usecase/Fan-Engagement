//
//  AgoraExtension.swift
//  FanEngagement
//
//  Created by CavanSu on 2019/5/30.
//  Copyright © 2019 Agora. All rights reserved.
//

import Foundation
import AgoraRtcEngineKit
import AgoraRtmKit

let appId: String = <#APPID#>
var channelId: String = ""

extension AgoraRtcEngineKit {
    static func shared(_ delegate: AgoraRtcEngineDelegate? = nil) -> AgoraRtcEngineKit {
        let kit = AgoraRtcEngineKit.sharedEngine(withAppId: appId, delegate: nil)
        kit.delegate = delegate
        return kit
    }
    
    func fanMode() {
        self.enableVideo()
        self.setChannelProfile(.liveBroadcasting)
        
        #if IS_BROADCASTER
        self.setClientRole(.broadcaster)
        #else
        self.setClientRole(.audience)
        #endif
    }
}

struct RTMChannel {
    var rtm: AgoraRtmChannel
    var channelId: String
}

class AgoraRTMController {
    static var controller = AgoraRTMController()
    
    private let kit: AgoraRtmKit = AgoraRtmKit(appId: appId, delegate: nil)!
    private var currentChannel: RTMChannel?
    
    weak var delegate: AgoraRtmDelegate? {
        didSet {
            kit.agoraRtmDelegate = delegate
        }
    }
    
    weak var channelDelegate: AgoraRtmChannelDelegate? {
        didSet {
            currentChannel?.rtm.channelDelegate = channelDelegate
        }
    }
    
    static func shared(_ delegate: AgoraRtmDelegate? = nil) -> AgoraRTMController {
        controller.kit.agoraRtmDelegate = delegate
        return controller
    }
    
    func login(success: (() -> Void)? = nil) {
        #if IS_BROADCASTER
        let user = "fan-ios-broadcaster"
        #else
        let user = "fan-ios-audience"
        #endif
        kit.login(byToken: nil, user: user) { (error) in
            print("login error code: \(error.rawValue)")
            guard error == .ok else {
                return
            }
            if let success = success {
                success()
            }
        }
    }
    
    func joinChannel(_ channelId: String, delegate: AgoraRtmChannelDelegate? = nil) {
        guard let rtmChannel = self.kit.createChannel(withId: channelId, delegate: delegate) else {
            return
        }
        
        rtmChannel.join(completion: { [unowned self] (error) in
            print("join channel error code: \(error.rawValue)")
            guard error == .ok else {
                return
            }
            let channel = RTMChannel(rtm: rtmChannel, channelId: channelId)
            self.currentChannel = channel
        })
    }
    
    func sendChannelMessage(_ text: String) {
        guard let channel = currentChannel else {
            return
        }
        let message = AgoraRtmMessage(text: text)
        channel.rtm.send(message) { (errorCode) in
            print("send channel message error code: \(errorCode.rawValue)")
            guard errorCode != .ok else {
                return
            }
        }
    }
}
