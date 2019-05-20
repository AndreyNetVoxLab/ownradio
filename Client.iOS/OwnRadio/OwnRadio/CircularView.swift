//
//  CircularView.swift
//  CircleTest
//
//  Created by Roman Litoshko on 12/8/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//
//	Circle View, creation and configuration circle view

import Foundation
import UIKit

class CircularView: UIView {
	let circlePathLayer = CAShapeLayer()
	let circleRadius:CGFloat = 75.0
	var circleCenter = CGPoint.zero
	
	var progress:CGFloat {
		get {
			
			return circlePathLayer.strokeEnd
		}
		set {
			
			if (newValue > 1) {
				circlePathLayer.strokeEnd = 1
			} else if (newValue < 0) {
				circlePathLayer.strokeEnd = 0
			} else {

				circlePathLayer.strokeEnd = newValue
			}
		}
	
	}
	
	override init(frame: CGRect) {
		super.init(frame: frame)
		configure()
	}
	
	required init?(coder aDecoder: NSCoder) {
		super.init(coder: aDecoder)
		configure()
	}
	
	func configure() {
		circlePathLayer.frame = bounds
		circlePathLayer.lineWidth = 5
		circlePathLayer.fillColor = UIColor.clear.cgColor
		circlePathLayer.strokeColor = UIColor.init(red: 78/255, green: 173/255, blue: 226/255, alpha: 1).cgColor
		layer.addSublayer(circlePathLayer)
		backgroundColor = UIColor.clear
		progress = 0
	}
	
	func circleFrame() -> CGRect {
		var circleFrame = CGRect(x: 0, y: 0, width: 2*circleRadius, height: 2*circleRadius)
		
		circleCenter = CGPoint(x: circlePathLayer.bounds.midX - circleFrame.midX, y: circlePathLayer.bounds.midY - circleFrame.midY)
		
		circleFrame.origin.x = circlePathLayer.bounds.midX - circleFrame.midX
		circleFrame.origin.y = circlePathLayer.bounds.midY - circleFrame.midY
		return circleFrame
	}
	
	func circlePath() -> UIBezierPath {
		let rect = circleFrame()
		
//		circleCenter = CGPoint(x: circlePathLayer.bounds.midX, y: circlePathLayer.bounds.midY)  -CGFloat(M_PI_2)
		
		
		let circlePath = UIBezierPath(arcCenter: CGPoint(x:rect.midX , y:rect.midY), radius: CGFloat(rect.width/2), startAngle: -CGFloat(M_PI_2), endAngle: CGFloat(3*M_PI/2), clockwise: true)
		
		return circlePath // UIBezierPath(ovalIn: circleFrame())//
	}

	override func layoutSubviews() {
		super.layoutSubviews()
		circlePathLayer.frame = bounds
		circlePathLayer.path = circlePath().cgPath
	}
}
