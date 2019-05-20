//
//  SettingsViewController.swift
//  OwnRadio
//
//  Created by Alexandra Polunina on 26.07.17.
//  Copyright © 2017 Netvox Lab. All rights reserved.
//

import UIKit
import Foundation

class SettingsViewController: UITableViewController {
	
	@IBOutlet weak var maxMemoryLbl: UILabel!
	@IBOutlet weak var stepper: UIStepper!
	@IBOutlet weak var onlyWiFiSwitch: UISwitch!
	
	@IBOutlet weak var delAllTracksLbl: UILabel!
    @IBOutlet weak var tracksRatio: UISlider!
    @IBOutlet weak var ratioLabel: UILabel!
    
	@IBOutlet weak var fromFreeSpace: UILabel!

	@IBOutlet weak var delAllTracksCell: UITableViewCell!
	
	var player: AudioPlayerManager?
	var remoteAudioControls: RemoteAudioControls?
	//получаем таблицу с количеством треков сгруппированных по количестсву их прослушиваний
	var playedTracks: NSArray = CoreDataManager.instance.getGroupedTracks()
	
	let userDefaults = UserDefaults.standard
	let tracksUrlString = FileManager.applicationSupportDir().appending("/Tracks/")
	
	override func viewDidLoad() {
		super.viewDidLoad()
		

		onlyWiFiSwitch.isOn = (userDefaults.object(forKey: "trafficOptimize") as? Bool)!
		
		stepper.wraps = true
		stepper.autorepeat = true
		stepper.value = (userDefaults.object(forKey: "maxMemorySize") as? Double)!
		maxMemoryLbl.text = Int(stepper.value).description + "%"
		stepper.minimumValue = 10.0
		stepper.maximumValue = 50.0
		stepper.stepValue = 10.0

		let freeSpace = Int64(DiskStatus.folderSize(folderPath: self.tracksUrlString)) + Int64(DiskStatus.freeDiskSpaceInBytes)

		self.fromFreeSpace.text = "*от свободной памяти " + DiskStatus.GBFormatter(freeSpace) + " Gb"
		
		
		let tapDelAllTracks = UITapGestureRecognizer(target: self, action: #selector(self.tapDelAllTracks(sender:)))
		delAllTracksCell.isUserInteractionEnabled = true
		delAllTracksCell.addGestureRecognizer(tapDelAllTracks)
		
		
		var str = "" as NSString
		for track in playedTracks {
		let dict = track as! [String: Any]
		let countOfPlay = dict["countPlay"] as? Int
		let countOfTracks = dict["count"] as? Int
		if countOfPlay != nil && countOfTracks != nil {
			if str == "" {
				str = NSString(format: "Count play: %d - Count tracks: %d", countOfPlay! , countOfTracks!)
			} else {
				str = NSString(format: "%@ \nCount play: %d - Count tracks: %d", str, countOfPlay! , countOfTracks!)
			}
			}
		}
	
		
		//Инициализация регулятора выдачи треков
		let thumbImage = UIImage(named: "trackThumb")
		let size = CGSize(width: (thumbImage?.size.width)! * 1.5, height: (thumbImage?.size.height)! * 1.5)
		UIGraphicsBeginImageContextWithOptions(size, false, 2.0)
		thumbImage?.draw(in: CGRect(x: 0, y: 0, width: size.width, height: size.height))
		let newThumbImage = UIGraphicsGetImageFromCurrentImageContext()
		UIGraphicsEndImageContext()
		tracksRatio.setThumbImage(newThumbImage, for: .normal)
		let ratio = UserDefaults.standard.integer(forKey: "getTracksRatio")
        ratioLabel.text = "\(ratio)%"
		tracksRatio.setValue(Float(ratio), animated: false)
	}
	
	@IBAction func onlyWiFiSwitchValueChanged(_ sender: UISwitch) {
		UserDefaults.standard.set(onlyWiFiSwitch.isOn, forKey: "trafficOptimize")
		UserDefaults.standard.synchronize()
	}
	
	
	//Сохраняем настроки "занимать не более" и выводим актуальное значение при его изменении
	@IBAction func stepperValueChanged(_ sender: UIStepper) {
		maxMemoryLbl.text = Int(stepper.value).description + "%"
		UserDefaults.standard.set(stepper.value, forKey: "maxMemorySize")
		UserDefaults.standard.synchronize()
	}
	
	
	@IBAction func btnfillCacheClick(_ sender: UIButton) {
		guard currentReachabilityStatus != NSObject.ReachabilityStatus.notReachable else {
			return
		}
		
		DispatchQueue.global(qos: .background).async {
			Downloader.sharedInstance.fillCache()
		}
	}
    
	override func remoteControlReceived(with event: UIEvent?) {
		guard let remoteControls = remoteAudioControls else {
			print("Remote controls not set")
			return
		}
		remoteControls.remoteControlReceived(with: event)
	}
	
	override func viewDidAppear(_ animated: Bool) {
		self.navigationController?.interactivePopGestureRecognizer?.isEnabled = false
	}
	
	override func viewDidDisappear(_ animated: Bool) {
		self.navigationController?.interactivePopGestureRecognizer?.isEnabled = true
	}
	
	@objc func tapDelAllTracks(sender: UITapGestureRecognizer) {
		let dellAllTracksAlert = UIAlertController(title: "Удаление всех треков", message: "Вы уверены что хотите удалить все треки из кэша? Приложение не сможет проигрывать треки в офлайне пока не будет наполнен кэш.", preferredStyle: UIAlertControllerStyle.alert)
		
		dellAllTracksAlert.addAction(UIAlertAction(title: "ОК", style: .default, handler: { (action: UIAlertAction!) in
			self.player?.pauseSong {
				if let rootController = UIApplication.shared.keyWindow?.rootViewController {
					let navigationController = rootController as! UINavigationController
					
					if let radioViewContr = navigationController.topViewController  as? RadioViewController {
						radioViewContr.updateUI()
					}
				}
			}
			
			let tracksUrlString = FileManager.applicationSupportDir().appending("/Tracks/")
			// получаем содержимое папки Tracks
			if let tracksContents = try? FileManager.default.contentsOfDirectory(atPath: tracksUrlString ){
				
				for track in tracksContents {
					// проверка для удаления только треков
						if track.contains("mp3") {
						let path = tracksUrlString.appending(track)
						do{
							print(path)
							try FileManager.default.removeItem(atPath: path)
								
						} catch  {
							print("Ошибка при удалении файла  - \(error)")
						}
					}
				}
				
				//удаляем треки из базы
				CoreDataManager.instance.deleteAllTracks()
				CoreDataManager.instance.saveContext()
				self.viewDidLoad()
			}
			
			
		}))
		
		dellAllTracksAlert.addAction(UIAlertAction(title: "ОТМЕНА", style: .cancel, handler: { (action: UIAlertAction!) in
			
		}))
		
		present(dellAllTracksAlert, animated: true, completion: nil)
		
		
	}
	
	@IBAction func delListenTracksBtn(_ sender: UIButton) {
		let dellListenTracksAlert = UIAlertController(title: "Удаление прослушанных треков", message: "Вы хотите удалить прослушанные треки из кэша?", preferredStyle: UIAlertControllerStyle.alert)
		
		dellListenTracksAlert.addAction(UIAlertAction(title: "ОК", style: .default, handler: { (action: UIAlertAction!) in
			
			let tracksUrlString =  FileManager.applicationSupportDir().appending("/Tracks/")
			
			let listenTracks = CoreDataManager.instance.getListenTracks()
			print("\(listenTracks.count)")
			for _track in listenTracks {
				let songObjectEncoded = UserDefaults.standard.data(forKey: "playingSongObject")
				let currentSongObject = try! PropertyListDecoder().decode(SongObject.self, from: songObjectEncoded!)
				if _track.trackID.isEqual(currentSongObject.trackID) == false{
					let path = tracksUrlString.appending((_track.path!))
					
					if FileManager.default.fileExists(atPath: path) {
						do{
							// удаляем файл
							try FileManager.default.removeItem(atPath: path)
						}
						catch {
							print("Ошибка при удалении файла - \(error)")
						}
					}
					// удаляем трек с базы
					CoreDataManager.instance.deleteTrackFor(trackID: _track.trackID)
					CoreDataManager.instance.saveContext()
				}
				
			}
			
			self.viewDidLoad()
		}))
		
		dellListenTracksAlert.addAction(UIAlertAction(title: "ОТМЕНА", style: .cancel, handler: { (action: UIAlertAction!) in
		}))
		
		present(dellListenTracksAlert, animated: true, completion: nil)
	}
	
	@IBAction func writeToDevelopers(_ sender: UIButton) {
		UIApplication.shared.openURL(NSURL(string: "http://www.vk.me/write-87060547")! as URL)
	}
	
	@IBAction func tapAction(_ sender: Any) {
		UserDefaults.standard.set(Int(Date().timeIntervalSince1970), forKey:  "setTimerDate")
	}
	
	@IBAction func rateAppBtn(_ sender: UIButton) {
		UIApplication.shared.openURL(NSURL(string: "itms://itunes.apple.com/ru/app/ownradio/id1179868370")! as URL)
	}
    @IBAction func ratioChangeAction(_ sender: UISlider) {
		sender.setValue(sender.value.rounded(.down), animated: true)
		UserDefaults.standard.set(Int(tracksRatio.value.rounded(.down)), forKey: "getTracksRatio")
        ratioLabel.text = "\(Int(tracksRatio.value.rounded(.down)))%"
    }
    
    
//	override func tableView(_ tableView: UITableView, heightForRowAt indexPath: IndexPath) -> CGFloat {
////		if indexPath.section == 4 && indexPath.row == 0 {
////
////			return 100
////		} else {
////			let row = tableView.cellForRow(at: indexPath)// dequeueReusableCell(withIdentifier: "Cell")//(at: indexPath) //.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
////			let h = row?.bounds.size.height
////			print (h ?? 1)
//			return UITableViewAutomaticDimension
////		}
//	}
	// MARK: UITableViewDataSource
	//	 override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
	////		if section == 4 {
	//			return self.playedTracks.count-1
	////		}
	////		if (section == 0) {
	////			return 1;
	////		} else {
	////			var frcSection = section - 1;
	////			id <NSFetchedResultsSectionInfo> sectionInfo = [[self.fetchedResultsController sections] objectAtIndex:frcSection];
	////			return sectionInfo numberOfObjects];
	////		}
	//	}
	
//		 override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
//			let cell = tableView.cellForRow(at: indexPath) //countListeningTableView.dequeueReusableCell(withIdentifier: "Cell", for: indexPath)
//			
////			let dict = playedTracks[indexPath.row] as! [String: Any]
////			let countOfPlay = dict["countPlay"] as? Int
////			let countOfTracks = dict["count"] as? Int
////			if countOfPlay != nil && countOfTracks != nil {
////				let str = NSString(format: "Count play: %d - Count tracks: %d", countOfPlay! , countOfTracks! )
////				cell.textLabel?.text = str as String
////			}
//			return cell
//		}
}
