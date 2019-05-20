//
//  AboutAppViewController.swift
//  OwnRadio
//
//  Created by Alexandra Polunina on 28.07.17.
//  Copyright © 2017 Netvox Lab. All rights reserved.
//

import UIKit
import Foundation

class AboutAppViewController: UIViewController{

    @IBOutlet weak var appVersionLbl: UILabel!
    
    @IBOutlet weak var scrollView: UIScrollView!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
            if (Bundle.main.infoDictionary?["CFBundleVersion"] as? String) != nil {
                self.appVersionLbl.text =  "v" + version
            }
        }
    }
}
