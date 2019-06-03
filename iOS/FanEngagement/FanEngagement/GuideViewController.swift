//
//  GuideViewController.swift
//  Broadcast
//
//  Created by CavanSu on 2019/5/30.
//  Copyright © 2019 Agora. All rights reserved.
//

import UIKit
import AgoraRtcEngineKit

struct Game {
    var name: String
    var url: String
    
    static var list: [Game] = {
        var guides = [Game]()
        guides.append(Game(name: "NBA1", url: "rtmp://vid-218.pull.chinanetcenter.broadcastapp.agora.io/live/1234"))
        guides.append(Game(name: "NBA2", url: "2"))
        guides.append(Game(name: "NBA3", url: "3"))
        return guides
    }()
    
    static var first: Game {
        return Game.list.first!
    }
    
    static func ==(left: Game, right: Game) -> Bool {
        return (left.url == right.url) ? true : false
    }
    
    static func !=(left: Game, right: Game) -> Bool {
        return (left.url != right.url) ? true : false
    }
}

class GuideCell: UITableViewCell {
    @IBOutlet weak var titleLabel: UILabel!
    @IBOutlet weak var colorView: UIView!
    
    func update(_ model: Game, isSelected: Bool) {
        titleLabel.text = model.name
        titleLabel.textColor = (isSelected ? UIColor.AGTextBlack : UIColor.AGTextGray)
        colorView.backgroundColor = (isSelected ? UIColor.AGLightGray : UIColor.AGLightBlack)
    }
}

class GuideViewController: UIViewController {
    @IBOutlet weak var tableView: UITableView!
    @IBOutlet weak var localView: UIView!
    
    private lazy var agoraKit: AgoraRtcEngineKit = {
        let kit = AgoraRtcEngineKit.shared()
        kit.fanMode()
        return kit
    }()
    
    private lazy var list = Game.list
    private var selectedGame = Game.first
    
    override func viewDidLoad() {
        super.viewDidLoad()
        addLocalPreview()
        agoraKit.startPreview()
    }
    
    deinit {
        agoraKit.stopPreview()
    }
}

private extension GuideViewController {
    func addLocalPreview() {
        let canvas = AgoraRtcVideoCanvas()
        canvas.renderMode = .hidden
        canvas.uid = 0
        canvas.view = self.localView
        agoraKit.setupLocalVideo(canvas)
    }
}

extension GuideViewController: UITableViewDataSource {
    func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return list.count
    }
    
    func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "GuideCell") as! GuideCell
        let model = list[indexPath.row]
        let isSelected = (selectedGame == model)
        cell.update(model, isSelected: isSelected)
        return cell
    }
}

extension GuideViewController: UITableViewDelegate {
    func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        let newGame = list[indexPath.row]
        guard newGame != selectedGame else {
            return
        }
        
        guard let index = list.firstIndex(where: { $0 == selectedGame }) else {
            return
        }
        selectedGame = newGame
        
        let oldSelectedIndex = Int(index)
        let oldIndex = IndexPath(row: oldSelectedIndex, section: 0)
        let newIndex = indexPath
        let needReloads = [oldIndex, newIndex]
        
        tableView.reloadRows(at: needReloads, with: .automatic)
    }
}

extension GuideViewController {
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        guard let identifier = segue.identifier, identifier == "guideToLive" else {
            return
        }
       
        let liveVC = segue.destination as? LiveViewController
        liveVC?.delegate = self
        liveVC?.dataSource = self
    }
}

extension GuideViewController: LiveVCDelegate {
    func liveVCWillLeave() {
        addLocalPreview()
    }
}

extension GuideViewController: LiveVCDataSource {
    func liveVCNeedSeletedGame() -> Game {
        return selectedGame
    }
}
