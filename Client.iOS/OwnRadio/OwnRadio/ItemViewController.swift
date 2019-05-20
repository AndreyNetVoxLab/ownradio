//
//  ItemViewController.swift
//  OwnRadio
//
//  Created by Alexandra Polunina on 03.08.17.
//  Copyright © 2017 Netvox Lab. All rights reserved.
//

import UIKit

class ItemViewController: UIViewController {

	var isBtnHide = true
	var isIndicatorHide  = true
	var isCountTryingHide = true
	var itemIndex: Int = 0
	var countTrying: Int = 0
	var labelText: String = "" {
		
		didSet {
			if let label = contentLbl {
				label.text = labelText
			}
		}
	}

	@IBOutlet var contentLbl: UILabel!
	@IBOutlet var tryAgainBtn: UIButton!
	@IBOutlet var pageControl: UIPageControl!
	@IBOutlet var countTryingLbl: UILabel!

	@IBOutlet var loadIndicator: UIActivityIndicatorView!
	
    override func viewDidLoad() {
        super.viewDidLoad()
		contentLbl.font = UIFont.boldSystemFont(ofSize: contentLbl.font.pointSize)
		contentLbl.text = labelText
		tryAgainBtn.isHidden = isBtnHide
		tryAgainBtn.layer.cornerRadius = 4
		loadIndicator.isHidden = isIndicatorHide
		countTryingLbl.isHidden = isCountTryingHide
		
//		pageControl.numberOfPages = StartVideoViewController.sharedInstance.contentLabels.count
		pageControl.currentPage = itemIndex
		
		countTryingLbl.text = "(\(countTrying) попытка кеширования)"
    }
	
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
	@IBAction func tryAgainBtnClick(_ sender: UIButton) {
		guard currentReachabilityStatus != NSObject.ReachabilityStatus.notReachable else {
			return
		}
		loadIndicator.isHidden = false
		tryAgainBtn.isHidden = true
		countTryingLbl.isHidden = false
//		contentLbl.text = StartVideoViewController.sharedInstance.getContentLblByIndex(index: itemIndex)
//		StartVideoViewController.sharedInstance.timerStart()
		DispatchQueue.global(qos: .background).async {
			Downloader.sharedInstance.load(isSelfFlag: false, complition: {
				
			})
		}
	}

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
    }
    */

}
