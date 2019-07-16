//
//  IndexViewController.swift
//  Broadcaster
//
//  Created by 张乾泽 on 2019/7/17.
//  Copyright © 2019 CavanSu. All rights reserved.
//

import Foundation
import UIKit

class IndexViewController: UIViewController {
    @IBOutlet weak var inputField: UITextField!
    
    override var prefersStatusBarHidden: Bool {
        return false
    }
    
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        channelId = inputField.text ?? ""
    }
}
