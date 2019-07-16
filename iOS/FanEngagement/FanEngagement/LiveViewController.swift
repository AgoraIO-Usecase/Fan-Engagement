//
//  LiveViewController.swift
//  FanEngagement
//
//  Created by CavanSu on 2019/5/29.
//  Copyright © 2019 Agora. All rights reserved.
//

import UIKit
import AgoraRtcEngineKit
import AgoraRtmKit

#if IS_BROADCASTER
protocol LiveVCDataSource: NSObjectProtocol {
    func liveVCNeedSeletedGame() -> Game 
}

protocol LiveVCDelegate: NSObjectProtocol {
    func liveVCWillLeave()
}
#endif

struct User {
    var uid:UInt
    var muted:Bool
    
    init(uid: UInt) {
        self.uid = uid
        muted = false
    }
}

struct Message {
    var sender: String
    var content: String
}

class MessageCell: UITableViewCell {
    @IBOutlet weak var contentLabel: UILabel!
    
    override func awakeFromNib() {
        super.awakeFromNib()
        // Initialization code
    }
}

class LiveViewController: UIViewController {
    @IBOutlet weak var broadcasterTagImageView: UIImageView!
    @IBOutlet weak var broadcasterView: UIView!
    
    @IBOutlet weak var linkHostButton: UIButton!
    @IBOutlet weak var localView: UIView!
    
    @IBOutlet weak var rtmpView: UIView!
    @IBOutlet weak var closeButton: UIButton!
    @IBOutlet weak var chatView: UITableView!
    @IBOutlet weak var toggleBtn: UIButton!
    @IBOutlet weak var fullscreenBtn: UIButton!
    
    private lazy var agoraKit: AgoraRtcEngineKit = {
        let kit = AgoraRtcEngineKit.shared(self)
        #if !IS_BROADCASTER
        kit.fanMode()
        #endif
        return kit
    }()
    
    private lazy var rtmKit: AgoraRTMController = {
        let kit = AgoraRTMController.shared()
        #if IS_BROADCASTER
        kit.channelDelegate = self
        #endif
        return kit
    }()
    
    private var isMuteAudio: Bool = false {
        didSet {
            agoraKit.muteLocalAudioStream(isMuteAudio)
            adjustRTMPVolume()
        }
    }
    
    private var isChatCollapsed: Bool = true {
        didSet {
            let constraint = view.constraints.first { $0.identifier == "chat.right.offset" }
            constraint?.constant = isChatCollapsed ? -184 : 0
            chatView.setNeedsUpdateConstraints()
            toggleBtn.setImage(isChatCollapsed ? UIImage(named: "expand") : UIImage(named: "collapse"), for: .normal)
        }
    }
    
    private var isFullscreen: Bool = false {
        didSet {
            let constraint = view.constraints.first { $0.identifier == "broadcaster.left.offset" }
            constraint?.constant = isFullscreen ? -175 : 20
            view.setNeedsUpdateConstraints()
            fullscreenBtn.setImage(isFullscreen ? UIImage(named: "quitfullscreen") : UIImage(named: "fullscreen"), for: .normal)
        }
    }
    
    private var isLowerRTMPAudio: Bool = false {
        didSet {
            let volume = isLowerRTMPAudio ? 5 : 90
            let parameter = "{\"che.audio.playout.uid.volume\": {\"uid\": 666, \"volume\": \(volume)}}"
            agoraKit.setParameters(parameter)
        }
    }
    
    private let gameStreamingUid: UInt = 666
    private var users:[User] = [] {
        didSet {
            adjustRTMPVolume()
        }
    }
    private var messages:[Message] = []
    private var barrageVC: BarrageViewController?
    private var textVC: TextViewController?
    
    #if IS_BROADCASTER
    weak var delegate: LiveVCDelegate?
    weak var dataSource: LiveVCDataSource?
    #endif
    
    override func viewDidLoad() {
        super.viewDidLoad()
        chatView.contentInset.top = 20
        chatView.contentInset.bottom = 20
        updateViews()
        joinChannel()
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        guard let identifier = segue.identifier else {
            return
        }
        
        switch identifier {
        case "liveEmbedBarrage":
            let vc = segue.destination as? BarrageViewController
            vc?.deleage = self
            barrageVC = vc
        case "liveEmbedText":
            let vc = segue.destination as? TextViewController
            vc?.delegate = self
            vc?.startListeningKeyboard()
            textVC = vc
        default:
            break
        }
    }
    
    @IBAction func doClosePressed(_ sender: UIButton) {
        #if IS_BROADCASTER
        leaveChannel()
        delegate?.liveVCWillLeave()
        navigationController?.popViewController(animated: true)
        #endif
    }
    
    @IBAction func doLinkHostPressed(_ sender: UIButton) {
        #if !IS_BROADCASTER
        agoraKit.setClientRole(.broadcaster)
        addLocalPreview()
        #endif
    }
    
    @IBAction func doMuteAudioPressed(_ sender: UIButton) {
        sender.isSelected.toggle()
        isMuteAudio.toggle()
    }
    
    @IBAction func doToggleChatView(_ sender: UIButton) {
        isChatCollapsed.toggle()
    }
    
    @IBAction func doToggleFullscreen(_ sender:UIButton) {
        isFullscreen.toggle()
    }
}

extension LiveViewController:UITableViewDataSource, UITableViewDelegate {
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell:MessageCell = tableView.dequeueReusableCell(withIdentifier: "MSG_CELL") as! MessageCell
        let msg = messages[indexPath.row]
        let uidAttributes = [NSAttributedString.Key.foregroundColor: UIColor(hexString: "#3575E2")] as [NSAttributedString.Key : Any]
        let contentAttributes = [NSAttributedString.Key.foregroundColor: UIColor.white] as [NSAttributedString.Key : Any]
        let attrstr = NSMutableAttributedString(string: "\(msg.sender)   ", attributes:uidAttributes)
        attrstr.append(NSAttributedString(string: msg.content, attributes: contentAttributes))
        cell.contentLabel?.attributedText = attrstr
        return cell
    }
    
    func numberOfSections(in tableView: UITableView) -> Int {
        return 1
    }
    
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return messages.count
    }
    
    
}

private extension LiveViewController {
    func updateViews() {
        #if IS_BROADCASTER
        addLocalPreview()
        #else
        linkHostButton.isHidden = false
        closeButton.isHidden = true
        #endif
    }
    
    func joinChannel() {
        agoraKit.joinChannel(byToken: nil, channelId: channelId, info: nil, uid: 0, joinSuccess: nil)
        
        #if !IS_BROADCASTER
        rtmKit.login { [unowned self] in
            self.rtmKit.joinChannel(channelId, delegate: self)
        }
        #endif
    }
    
    func leaveChannel() {
        agoraKit.leaveChannel(nil)
        removeRTMPStreaming()
    }
    
    func addLocalPreview() {
        let canvas = AgoraRtcVideoCanvas()
        canvas.renderMode = .hidden
        canvas.uid = 0
        canvas.view = self.localView
        agoraKit.setupLocalVideo(canvas)
    }
    
    func pullRTMPStreaming() {
        #if IS_BROADCASTER
        guard let gameURL = dataSource?.liveVCNeedSeletedGame().url else {
            return
        }
        let config = AgoraLiveInjectStreamConfig()
        agoraKit.addInjectStreamUrl(gameURL, config: config)
        #endif
    }
    
    func removeRTMPStreaming() {
        #if IS_BROADCASTER
        guard let gameURL = dataSource?.liveVCNeedSeletedGame().url else {
            return
        }
        agoraKit.removeInjectStreamUrl(gameURL)
        #endif
    }
    
    func setupRTMPVideoStream() {
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = gameStreamingUid
        canvas.view = self.rtmpView
        canvas.renderMode = .hidden
        agoraKit.setupRemoteVideo(canvas)
    }
    
    func setupRemoteVideoStream(uid: UInt) {
        broadcasterTagImageView.isHidden = true
        
        let canvas = AgoraRtcVideoCanvas()
        canvas.uid = uid
        canvas.view = self.broadcasterView
        canvas.renderMode = .hidden
        agoraKit.setupRemoteVideo(canvas)
    }
    
    func remoteVideoStreamOffline(uid: UInt) {
        broadcasterTagImageView.isHidden = false
    }
    
    func adjustRTMPVolume() {
        //if all other users muted, recover volume; otherwise keep low
        let unmutedUsers = users.filter { (user) -> Bool in
            return !user.muted
        }
        isLowerRTMPAudio = !isMuteAudio ||  unmutedUsers.count > 0 ? true : false
    }
}

extension LiveViewController: AgoraRtcEngineDelegate {
    func rtcEngine(_ engine: AgoraRtcEngineKit, didJoinChannel channel: String, withUid uid: UInt, elapsed: Int) {
        pullRTMPStreaming()
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, firstRemoteVideoDecodedOfUid uid: UInt, size: CGSize, elapsed: Int) {
        print("firstRemoteVideoDecodedOf uid: \(uid)")
        
        if uid == gameStreamingUid {
            setupRTMPVideoStream()
        } else {
            setupRemoteVideoStream(uid: uid)
        }
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, firstRemoteAudioFrameDecodedOfUid uid: UInt, elapsed: Int) {
        // add uid into users array
        users.append(User(uid: uid))
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, didOfflineOfUid uid: UInt, reason: AgoraUserOfflineReason) {
        remoteVideoStreamOffline(uid: uid)
        //remove uid from users array
        guard let idx = users.firstIndex(where: { (user) -> Bool in
            return user.uid == uid
        }) else {return}
        users.remove(at: idx)
    }
    
    func rtcEngine(_ engine: AgoraRtcEngineKit, streamInjectedStatusOfUrl url: String, uid: UInt, status: AgoraInjectStreamStatus) {
        print("streamInjectedStatusOfUrl status: \(status.rawValue)")
    }

    func rtcEngine(_ engine: AgoraRtcEngineKit, didAudioMuted muted: Bool, byUid uid: UInt) {
        print ("the uid :\(uid) is muteLocalAudioStream")
        
        //set mute flag for uid
        guard var user = users.first(where: { (user) -> Bool in
            return user.uid == uid
        }) else {return}
        
        user.muted = muted
        adjustRTMPVolume()
    }
}
extension LiveViewController: AgoraRtmChannelDelegate {
    func channel(_ channel: AgoraRtmChannel, messageReceived message: AgoraRtmMessage, from member: AgoraRtmMember) {
        barrageVC?.lauchBarrage(text: message.text)
        
        //show chat message in right panel
        messages.append(Message(sender: member.userId, content: message.text))
        chatView.beginUpdates()
        chatView.insertRows(at: [IndexPath(row: messages.count - 1, section: 0)], with: .automatic)
        chatView.endUpdates()
    }
}

extension LiveViewController {
    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        view.endEditing(true)
    }
}

extension LiveViewController: BarrageVCDelegate {
    func barrageVCDidTapView() {
        view.endEditing(true)
    }
}

extension LiveViewController: TextVCDelegate {
    func textVCWillShow() {
    }
    
    func textVCWillHide(string: String?) {
        if let text = string {
            barrageVC?.lauchBarrage(text: text)
            rtmKit.sendChannelMessage(text)
        }
    }
}
