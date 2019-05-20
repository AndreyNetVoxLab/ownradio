//
//  DevInfoTableViewController.swift
//  OwnRadio
//
//  Created by Alexandr Serov on 07.05.2019.
//  Copyright © 2019 Netvox Lab. All rights reserved.
//

import UIKit

class DevInfoTableViewController: UITableViewController {

    @IBOutlet weak var totalMemoryLbl: UILabel!
    @IBOutlet weak var cachedSizeLbl: UILabel!
    @IBOutlet weak var listenTracksCount: UILabel!
    @IBOutlet weak var cachedTracksCount: UILabel!
    @IBOutlet weak var memoryAvailable: UILabel!
	@IBOutlet weak var userIdLbl: UILabel!
	@IBOutlet weak var deviceIdLbl: UILabel!
    @IBOutlet weak var userNameLbl: UILabel!
    
    
    override func viewDidLoad() {
        super.viewDidLoad()
		let tracksUrlString = FileManager.applicationSupportDir().appending("/Tracks/")
		
		let availablememoryPercent = UserDefaults.standard.integer(forKey: "maxMemorySize")
		let cacheFolderSize = DiskStatus.folderSize(folderPath: tracksUrlString)
		let totalSpace = cacheFolderSize + DiskStatus.freeDiskSpaceInBytes
		
		let memoryAvailable = Int64((Float(availablememoryPercent) / 100) * Float(totalSpace))
		
		let listenTracks = CoreDataManager.instance.getListenTracks()
		
		
		self.totalMemoryLbl.text = "Свободно \(DiskStatus.GBFormatter(Int64(totalSpace))) GB"
		self.cachedSizeLbl.text = "Занято \(DiskStatus.GBFormatter(Int64(cacheFolderSize))) GB"
		self.listenTracksCount.text = "Прослушано \(listenTracks.count.description) треков"
		self.cachedTracksCount.text = "Кэшировано \(CoreDataManager.instance.chekCountOfEntitiesFor(entityName: "TrackEntity").description) треков"
		self.memoryAvailable.text = "Доступно памяти \(DiskStatus.GBFormatter(memoryAvailable).description) GB"
		
		RdevApiService().GetDeviceInfo { (deviceInfo) in
			if let userid = deviceInfo["userid"]{
				let deviceid = UserDefaults.standard.string(forKey: "deviceIdentifier")
				
				self.userIdLbl.text = userid
				self.deviceIdLbl.text = deviceid
                self.userNameLbl.text = deviceInfo["recname"]
			}else{
				self.userIdLbl.text = "Ошибка получения userid"
				self.deviceIdLbl.text = "Ошибка получения userid"
                self.userNameLbl.text = "Ошибка получения имени пользователя"
			}
		}
		
		
    }

    // MARK: - Table view data source

//    override func numberOfSections(in tableView: UITableView) -> Int {
//        // #warning Incomplete implementation, return the number of sections
//        return 1
//    }
//
//    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
//        // #warning Incomplete implementation, return the number of rows
//        return 5
//    }

    /*
    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "reuseIdentifier", for: indexPath)

        // Configure the cell...

        return cell
    }
    */

    /*
    // Override to support conditional editing of the table view.
    override func tableView(_ tableView: UITableView, canEditRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the specified item to be editable.
        return true
    }
    */

    /*
    // Override to support editing the table view.
    override func tableView(_ tableView: UITableView, commit editingStyle: UITableViewCellEditingStyle, forRowAt indexPath: IndexPath) {
        if editingStyle == .delete {
            // Delete the row from the data source
            tableView.deleteRows(at: [indexPath], with: .fade)
        } else if editingStyle == .insert {
            // Create a new instance of the appropriate class, insert it into the array, and add a new row to the table view
        }    
    }
    */

    /*
    // Override to support rearranging the table view.
    override func tableView(_ tableView: UITableView, moveRowAt fromIndexPath: IndexPath, to: IndexPath) {

    }
    */

    /*
    // Override to support conditional rearranging of the table view.
    override func tableView(_ tableView: UITableView, canMoveRowAt indexPath: IndexPath) -> Bool {
        // Return false if you do not want the item to be re-orderable.
        return true
    }
    */

    /*
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepare(for segue: UIStoryboardSegue, sender: Any?) {
        // Get the new view controller using segue.destination.
        // Pass the selected object to the new view controller.
    }
    */

}
