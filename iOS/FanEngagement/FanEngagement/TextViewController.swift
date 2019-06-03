//
//  TextViewController.swift
//  FanEngagement
//
//  Created by CavanSu on 2019/6/2.
//  Copyright © 2019 Agora. All rights reserved.
//

import UIKit

protocol TextVCDelegate: NSObjectProtocol {
    func textVCWillShow()
    func textVCWillHide(string: String?)
}

class TextViewController: UIViewController {
    @IBOutlet weak var inputTextField: UITextField!
    
    weak var delegate: TextVCDelegate?
    
    func startListeningKeyboard() {
        addKeyboradObserver()
    }
    
    func stopListeningKeyboard() {
        NotificationCenter.default.removeObserver(self)
    }
    
    deinit {
        stopListeningKeyboard()
    }
}

private extension TextViewController {
    func addKeyboradObserver() {
        NotificationCenter.default.addObserver(self,
                                               selector: #selector(keyboardFrameWillChange(notification:)),
                                               name: UIResponder.keyboardWillChangeFrameNotification, object: nil)
    }
    
    @objc func keyboardFrameWillChange(notification: NSNotification) {
        guard let userInfo = notification.userInfo,
            let endKeyboardFrameValue = userInfo[UIResponder.keyboardFrameEndUserInfoKey] as? NSValue,
            let durationValue = userInfo[UIResponder.keyboardAnimationDurationUserInfoKey] as? NSNumber else {
                return
        }
        
        let endKeyboardFrame = endKeyboardFrameValue.cgRectValue
        let duration = durationValue.doubleValue
        
        let isShowing: Bool = endKeyboardFrame.maxY > UIScreen.height ? false : true
        
        UIView.animate(withDuration: duration) { [weak self] in
            guard let strongSelf = self else {
                return
            }
            
            if isShowing {
                guard let sView = strongSelf.view.superview else {
                    return
                }
                
                let offsetY = sView.frame.maxY - endKeyboardFrame.minY
                sView.transform = CGAffineTransform(translationX: 0, y: -offsetY)
            } else {
                strongSelf.view.superview?.transform = .identity
            }
        }
        
        if isShowing {
            delegate?.textVCWillShow()
            inputTextField.becomeFirstResponder()
        } else {
            delegate?.textVCWillHide(string: inputTextField?.text)
            inputTextField.resignFirstResponder()
        }
        self.view.superview?.isHidden = !isShowing
    }
}

extension TextViewController: UITextFieldDelegate {
    func textFieldShouldReturn(_ textField: UITextField) -> Bool {
        let text = textField.text
        textField.text = nil
        delegate?.textVCWillHide(string: text)
        return true
    }
}
