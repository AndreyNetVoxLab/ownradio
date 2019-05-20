//
//  DiskStatus.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 12/7/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//
//	Methods for check memmory

import Foundation

class DiskStatus {
	
	//MARK: Formatter MB only
	class func MBFormatter(_ bytes: Int64) -> String {
//		let startTime = CFAbsoluteTimeGetCurrent()
		
		let formatter = ByteCountFormatter()
		formatter.allowedUnits = ByteCountFormatter.Units.useMB
		formatter.countStyle = ByteCountFormatter.CountStyle.decimal
		formatter.includesUnit = false
		
//		let endTime = CFAbsoluteTimeGetCurrent() - startTime
//		print("time execute for MBformatter: \(String.localizedStringWithFormat("%.2f", endTime))")
		
		return formatter.string(fromByteCount: bytes) as String
	}
    
    //MARK: Formatter GB only
    class func GBFormatter(_ bytes: Int64) -> String {
//		let startTime = CFAbsoluteTimeGetCurrent()
		
        let formatter = ByteCountFormatter()
        formatter.allowedUnits = ByteCountFormatter.Units.useGB
        formatter.countStyle = ByteCountFormatter.CountStyle.decimal
        formatter.includesUnit = false
		
//		let endTime = CFAbsoluteTimeGetCurrent() - startTime
//		print("time execute for GBFormatter: \(String.localizedStringWithFormat("%.2f", endTime))")
        return formatter.string(fromByteCount: bytes) as String
    }
	
	//MARK: Get String Value
	class var totalDiskSpace:String {
		get {
			return ByteCountFormatter.string(fromByteCount: Int64(totalDiskSpaceInBytes), countStyle: ByteCountFormatter.CountStyle.binary)
		}
	}
	
	class var freeDiskSpace:String {
		get {
			return ByteCountFormatter.string(fromByteCount: Int64(freeDiskSpaceInBytes), countStyle: ByteCountFormatter.CountStyle.binary)
		}
	}
	
	class var usedDiskSpace:String {
		get {
			return ByteCountFormatter.string(fromByteCount: Int64(usedDiskSpaceInBytes), countStyle: ByteCountFormatter.CountStyle.binary)
		}
	}

	//MARK: Get raw value
	//возвращает общее количество памяти
	class var totalDiskSpaceInBytes:UInt64 {
		get {
			do {
//				let startTime = CFAbsoluteTimeGetCurrent()
				
				let systemAttributes = try FileManager.default.attributesOfFileSystem(forPath: NSHomeDirectory() as String)
				let space = (systemAttributes[FileAttributeKey.systemSize] as? NSNumber)?.uint64Value
				
//				let endTime = CFAbsoluteTimeGetCurrent() - startTime
//				print("time execute for totalDiskSpaceInBytes: \(String.localizedStringWithFormat("%.2f", endTime))")
				return space!
			} catch {
				return 0
			}
		}
	}
	
	//возвращает количество памяти, занимаемое треками
	class func folderSize(folderPath:String) -> UInt64{
		
//		let startTime = CFAbsoluteTimeGetCurrent()
		
		let filesArray:[String]? = try? FileManager.default.subpathsOfDirectory(atPath: folderPath.appending("/")) as [String]
		var fileSize:UInt64 = 0
		
		for fileName in filesArray!{
			
			let str  =  folderPath.appending(fileName)  //folderPath.addingPercentEncoding(withAllowedCharacters:.urlUserAllowed)
//			let folderUrl = NSURL(fileURLWithPath: str)
//			let filePath = folderUrl.appendingPathComponent(fileName)?.absoluteString
			do {
				let fileDictionary:NSDictionary = try FileManager.default.attributesOfItem(atPath: str) as NSDictionary
				fileSize += UInt64(fileDictionary.fileSize())
			} catch {
				print(error.localizedDescription)
			}
		}
		
//		let endTime = CFAbsoluteTimeGetCurrent() - startTime
//		print("time execute for folderSize: \(String.localizedStringWithFormat("%.2f", endTime))")
		
		return fileSize
	}
	
	//возвращает количество свободной памяти
	class var freeDiskSpaceInBytes:UInt64 {
		get{
			
//			let startTime = CFAbsoluteTimeGetCurrent()
			
			let documentDirectory = NSSearchPathForDirectoriesInDomains(.documentDirectory, .userDomainMask, true).last!
			guard
				let systemAttributes = try? FileManager.default.attributesOfFileSystem(forPath: documentDirectory),
				let freeSize = systemAttributes[.systemFreeSize] as? NSNumber
				else {
					// something failed
					return 0
			}
			let corectSize = freeSize.doubleValue;
			
//			let endTime = CFAbsoluteTimeGetCurrent() - startTime
//			print("time execute for freeDiskSpaceInBytes: \(String.localizedStringWithFormat("%.2f", endTime))")
			
			return UInt64(corectSize)
		}
	}
	
	
	//возвращает общее количество занятой памяти
	class var usedDiskSpaceInBytes:UInt64 {
		get {
//			let startTime = CFAbsoluteTimeGetCurrent()
			
			let usedSpace = totalDiskSpaceInBytes - freeDiskSpaceInBytes
			
//			let endTime = CFAbsoluteTimeGetCurrent() - startTime
//			print("time execute for usedDiskSpaceInBytes: \(String.localizedStringWithFormat("%.2f", endTime))")
			
			return usedSpace
		}
	}
    
    //возвращает количество памяти, занимаемое треками
    class func listenTracksSize(folderPath:String, tracks:[SongObject]) -> UInt64{
		let startTime = CFAbsoluteTimeGetCurrent()
		
        let tracksUrlString =  FileManager.applicationSupportDir().appending("/Tracks/")
        var fileSize:UInt64 = 0
        
        for _track in tracks {
            let path = tracksUrlString.appending((_track.path!))
            
            if FileManager.default.fileExists(atPath: path) {
                do{
                    let fileDictionary:NSDictionary = try FileManager.default.attributesOfItem(atPath: path) as NSDictionary
                    fileSize += UInt64(fileDictionary.fileSize())
                } catch {
                    print(error.localizedDescription)
                }
            } else {
                print("Ошибка: файл не существует")
            }
        }
		
//		let endTime = CFAbsoluteTimeGetCurrent() - startTime
//		print("time execute for listenTracksSize: \(String.localizedStringWithFormat("%.2f", endTime))")
		
        return fileSize
    }
	
}

