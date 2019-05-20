//
//  CachingView.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 1/30/17.
//  Copyright © 2017 Roll'n'Code. All rights reserved.
//

import UIKit

class CachingView: UIView {
	
	@IBOutlet var activityIndicator: UIActivityIndicatorView!
	
	
	class func instanceFromNib() -> UIView {
		return UINib(nibName: "CachingView", bundle: nil).instantiate(withOwner: nil, options: nil)[0] as! UIView
	}
	
	override func awakeFromNib() {
		super.awakeFromNib()
		animatedAppear()
		activityIndicator.startAnimating()
	}
	
	override func removeFromSuperview() {
		animatedRemovingFromSuperview()
	}
	
	func animatedRemovingFromSuperview () {
		DispatchQueue.main.async {
			UIView.animate(withDuration: 0.8, animations: {
				self.activityIndicator.stopAnimating()
				self.alpha = 0
			}) { (bollValue) in
				super.removeFromSuperview()
			}
		}
	}
	
	func animatedAppear() {
		DispatchQueue.main.async {
			self.alpha = 0
			UIView.animate(withDuration: 0.8) {
				self.alpha = 1
			}
		}
	}
	
}
