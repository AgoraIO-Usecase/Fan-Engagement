//
//  MainViewController.swift
//  FanEngagement
//
//  Created by CavanSu on 2019/5/29.
//  Copyright © 2019 Agora. All rights reserved.
//

import UIKit
import AgoraRtmKit

class MainViewController: UIViewController {
    
    override var prefersStatusBarHidden: Bool {
        return false
    }
    
    private var barrageVC: BarrageViewController?
    private var textVC: TextViewController?
    private lazy var rtmKit: AgoraRTMController = {
        return AgoraRTMController.shared()
    }()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        rtmKit.login { [unowned self] in
            self.rtmKit.joinChannel(channelId, delegate: self)
        }
    }
    
    override func viewDidAppear(_ animated: Bool) {
        super.viewDidAppear(animated)
        textVC?.startListeningKeyboard()
    }
    
    override func viewDidDisappear(_ animated: Bool) {
        super.viewDidDisappear(animated)
        textVC?.stopListeningKeyboard()
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        guard let identifier = segue.identifier else {
            return
        }
        
        switch identifier {
        case "mainEmbedBarrage":
            let vc = segue.destination as? BarrageViewController
            vc?.deleage = self
            barrageVC = vc
        case "mainEmbedText":
            let vc = segue.destination as? TextViewController
            vc?.delegate = self
            textVC = vc
        default:
            break
        }
    }
    
    @IBAction func doExitPressed(_ sender: UIStoryboardSegue) {
    }
}

extension MainViewController: AgoraRtmChannelDelegate {
    func channel(_ channel: AgoraRtmChannel, messageReceived message: AgoraRtmMessage, from member: AgoraRtmMember) {
        barrageVC?.lauchBarrage(text: message.text)
    }
}

extension MainViewController: BarrageVCDelegate {
    func barrageVCDidTapView() {
        view.endEditing(true)
    }
}

extension MainViewController: TextVCDelegate {
    func textVCWillShow() {
    }
    
    func textVCWillHide(string: String?) {
        if let text = string {
            barrageVC?.lauchBarrage(text: text)
            rtmKit.sendChannelMessage(text)
        }
    }
}

extension MainViewController {
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}
