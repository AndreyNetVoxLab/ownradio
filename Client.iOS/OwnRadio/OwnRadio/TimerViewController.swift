//
//  TimerViewController.swift
//  OwnRadio
//
//  Created by Alexandr Serov on 16.04.2019.
//  Copyright © 2019 Netvox Lab. All rights reserved.
//

import UIKit
import HGCircularSlider

class TimerViewController: UIViewController {

    
    @IBOutlet weak var circularSliderView: UIView!
    @IBOutlet weak var timerinfoLabel: UILabel!
    @IBOutlet weak var setTimerBTN: UIButton!
    @IBOutlet weak var setInfoLabel: UILabel!
    
    
    let defaults = UserDefaults.standard
	var currentSliderValue = 0
	var slider: CircularSlider = CircularSlider()
	var timer: DispatchSourceTimer?
	
    var remoteAudioControls: RemoteAudioControls?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        timerinfoLabel.text = ""
        setInfoLabel.text = ""
        createCircularSlider()
		
		if defaults.bool(forKey: "timerState") {
			setTimerBTN.setImage(UIImage(named: "blueTimer"), for: .normal)
			
			let updateTimerDate = UserDefaults.standard.integer(forKey: "updateTimerDate")
			let time: String //getRemainingTime()
			if updateTimerDate > UserDefaults.standard.integer(forKey: "setTimerDate") {
				time = getRemainingTime(interval: updateTimerDate)
				slider.endPointValue = CGFloat(Float(getRemainingTimeInterval(interval: updateTimerDate)) / 60)
			} else {
				time = getRemainingTime()
				slider.endPointValue = CGFloat(Float(getRemainingTimeInterval()) / 60)
			}
			let splittedTime = time.split(separator: ":")
			if splittedTime.count == 2 {
				setInfoLabel.text = "Таймер установлен\nприложение закроется через " + splittedTime[0] + " ч, " + splittedTime[1] + " мин"
			} else {
				setInfoLabel.text = "Таймер установлен\nприложение закроется через " + splittedTime[0] + " мин"
			}
			
		} else {
			setTimerBTN.setImage(UIImage(named: "grayTimer"), for: .normal)
		}
		sliderValueChanged(sender: slider)
    }
    
    override func remoteControlReceived(with event: UIEvent?) {
        guard let remoteControls = remoteAudioControls else{
            return
        }
        
        remoteControls.remoteControlReceived(with: event)
    }

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

    
    /// Создание круглого слайдера
    func createCircularSlider(){
        circularSliderView.backgroundColor = .clear
        
        var frame = circularSliderView.frame
        frame.origin.x = 0
        frame.origin.y = 0
        
        let grayColor = UIColor(red: 0.83, green: 0.83, blue: 0.83, alpha: 1)
        let blueColor = UIColor(red: 0.08, green: 0.60, blue: 0.92, alpha: 1)
        
        slider = CircularSlider(frame: frame)
        slider.maximumValue = 240.0
        slider.trackColor = grayColor
        slider.trackFillColor = blueColor
        slider.diskColor = .clear
        slider.diskFillColor = .clear
        slider.endThumbStrokeColor = .clear
        slider.endThumbTintColor = blueColor
        slider.endThumbStrokeHighlightedColor = blueColor
        slider.thumbRadius = 7
        slider.addTarget(self, action: #selector(sliderValueChanged), for: .valueChanged)
        slider.backgroundColor = .clear
        circularSliderView.addSubview(slider)
        sliderValueChanged(sender: slider)
    }
    
    @objc func sliderValueChanged(sender: CircularSlider){
        timerinfoLabel.text = timeIntervalToStr(interval: TimeInterval(Int(slider.endPointValue) * 60))
    }
    
    func timeIntervalToStr(interval: TimeInterval) -> String{
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute]
        formatter.unitsStyle = .positional
        let formattedString = formatter.string(from: interval)
        return formattedString ?? "0"
    }
    
    private func startTimer(timeInterval: TimeInterval){
        let queue = DispatchQueue(label: "AlertCloctTimer", attributes: .concurrent)
        timer?.cancel()
        timer = DispatchSource.makeTimerSource(queue: queue)
        
        timer?.scheduleOneshot(deadline: .now() + .seconds(Int(timeInterval)))
        timer?.setEventHandler{
            self.timerAction()
        }
        
        timer?.resume()
    }
    
    func timerAction(){
        if defaults.bool(forKey: "timerState") && !((timer?.isCancelled)!){
            var setTimerDate = defaults.integer(forKey: "setTimerDate")
            let updateTimerDate = defaults.integer(forKey: "updateTimerDate")
            
            if updateTimerDate > setTimerDate{
                setTimerDate = updateTimerDate
            }
            
            let timeInterval = Double(Int(slider.endPointValue) * 60)
            
            if (Int(Date().timeIntervalSince1970 - timeInterval) >= setTimerDate){
                defaults.set(false, forKey: "timerState")
                defaults.set(0, forKey: "timerDurationSeconds")
				exit(0)
            }else{
                let datesDifferent = Int(timeInterval - Date().timeIntervalSince1970) - updateTimerDate
                startTimer(timeInterval: TimeInterval(datesDifferent))
            }
        }
    }
	
	func getRemainingTimeInterval() -> Float {
		let currentDate = Date()
		let setTimerDate = UserDefaults.standard.integer(forKey: "setTimerDate")
		let timerDuration = UserDefaults.standard.integer(forKey: "timerDurationSeconds")
		let remainingTimerDuration = Float(Double(setTimerDate + timerDuration) - currentDate.timeIntervalSince1970)
		return remainingTimerDuration
	}
	
	func getRemainingTimeInterval(interval: Int) -> Float {
		let currentDate = Date()
		let timerDuration = UserDefaults.standard.integer(forKey: "timerDurationSeconds")
		let remainingTimerDuration = Float(Double(interval + timerDuration) - currentDate.timeIntervalSince1970)
		return remainingTimerDuration
	}
	
	func getRemainingTime() -> String {
		let currentDate = Date()
		let setTimerDate = UserDefaults.standard.integer(forKey: "setTimerDate")
		let timerDuration = UserDefaults.standard.integer(forKey: "timerDurationSeconds")
		let remainingTimerDuration = Double(setTimerDate + timerDuration) - currentDate.timeIntervalSince1970
		let formattedString = timeIntervalToStr(interval: remainingTimerDuration)
		return formattedString ?? "0"
	}
	
	func getRemainingTime(interval: Int) -> String {
		let currentDate = Int(Date().timeIntervalSince1970)
		//let setTimerDate = UserDefaults.standard.integer(forKey: "setTimerDate")
		let timerDuration = UserDefaults.standard.integer(forKey: "timerDurationSeconds")
		let remainingTimerDuration = interval + timerDuration - currentDate
		let formattedString = timeIntervalToStr(interval: TimeInterval(remainingTimerDuration))
		return formattedString ?? "0"
	}
    
    @IBAction func startTimerBtnAction(_ sender: Any) {
        if !defaults.bool(forKey: "timerState"){
            let seconds = Float(Int(slider.endPointValue) * 60)
            
            setTimerBTN.setImage(UIImage(named: "blueTimer"), for: .normal)
            defaults.set(true, forKey: "timerState")
            defaults.set(Int(Date().timeIntervalSince1970), forKey: "setTimerDate")
            defaults.set(Int(Date().timeIntervalSince1970), forKey: "updateTimerDate")
            defaults.set(seconds, forKey: "timerDurationSeconds")
            
            
            startTimer(timeInterval: TimeInterval(seconds))
            
            let stringTime = timeIntervalToStr(interval: TimeInterval(seconds))
            let splittedTime = stringTime.split(separator: ":")
            
            if splittedTime.count == 2{
                setInfoLabel.text = "Таймер установлен\nприложение закроется через " + splittedTime[0] + " ч, " + splittedTime[1] + " мин"
            }else{
                setInfoLabel.text = "Таймер установлен\nприложение закроется через " + splittedTime[0] + " мин"
            }
        }else{
            setTimerBTN.setImage(UIImage(named: "grayTimer"), for: .normal)
            defaults.set(false, forKey: "timerState")
            self.timer?.cancel()
            self.timer = nil
            
            setInfoLabel.text = "Таймер остановлен"
        }
    }
}
