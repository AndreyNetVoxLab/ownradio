//
//  RdevApiService.swift
//  OwnRadio
//
//  Created by Alexandr Serov on 19.03.2019.
//  Copyright © 2019 Netvox Lab. All rights reserved.
//

import Foundation
import UIKit
import Alamofire


class RdevApiService {
	
	let loginUrl = URL(string: "http://rdev.ownradio.ru/auth/login")
	let apiUrl = URL(string: "http://rdev.ownradio.ru/api/executejs")
	var countRequest:Int! = 0
	let userDefault = UserDefaults.standard
	static let shared = ApiService()
	
	init() {
	}
	
	//Получение токена аутентификации
	func GetAuthToken(completion: @escaping (String)->()){
		
		let json: Parameters = ["login": "admin",
									  "password" : "2128506"]
		//let jsonData = try? JSONSerialization.data(withJSONObject: json)
		
		let headers: HTTPHeaders = [
			"Content-Type":"application/json"
		]
		
		
		
		Alamofire.request(loginUrl!, method: .post, parameters: json, encoding: JSONEncoding.default, headers: headers).responseJSON { response in
			if let statusCode = response.response?.statusCode{
				if statusCode == 200{
					let json = response.result.value as! NSDictionary
					self.userDefault.set(json["token"] as! String, forKey: "authToken")
					//print(json["token"] as! String)
					return completion(json["token"] as! String)
				}
				else if statusCode > 300{
					NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetToken FAIL:\(statusCode.description)"])
					return completion(statusCode.description)
				}
				else{
					print("Token \(statusCode.description)")
					return completion(statusCode.description)
				}
			}
			else{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetToken FAIL:NotHttp"])
				return completion("NotHttp")
			}
			
		}
	}
	
	//Регистрация устройства
	func RegisterDevice(completion: @escaping (Bool)->()) {
		
		let systemVersion = UIDevice.current.systemVersion
		let model = UIDevice.current.model
		let deviceName: String? = model + " " + systemVersion
		var deviceId = userDefault.string(forKey: "deviceIdentifier")
		if deviceId == ""{
			deviceId = UIDevice.current.identifierForVendor?.uuidString.lowercased()
		}
		
		let token = userDefault.string(forKey: "authToken") as! String
		let json: Parameters = ["fields": ["recid": deviceId, "recname":deviceName ?? "New IOS device"], "method":"regnewdevice"]
		
		let headers: HTTPHeaders = [
			"Content-Type":"application/json",
			"Authorization":"Bearer \(token)"
		]
		
		Alamofire.request(apiUrl!, method: .post, parameters: json, encoding: JSONEncoding.default, headers: headers).responseJSON { response in
			if let statusCode = response.response?.statusCode{
				if statusCode == 200{
					NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Device registered"])
					self.userDefault.set(deviceId, forKey: "deviceIdentifier")
					self.userDefault.synchronize()
					return completion(true)
				}
				else if statusCode == 401{
					self.GetAuthToken(completion: { (_) in
						print("register \(statusCode)")
						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Register request unauth: \(statusCode.description)"])
						sleep(5)
						self.RegisterDevice(){comp in
							if comp{
								return completion(true)
							}else{
								return completion(false)
							}
						}
					})
					//return completion(false)
				}else{
					NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Register request fail: \(statusCode.description)"])
					return completion(false)
				}
			}else{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Register FAIL:NotHttp"])
				return completion(false)
			}
		}
	}
	
	//Получение инфы о следующем треке
	func GetTrackInfo(requestCount: Int, complition:  @escaping ([String:Any]) -> Void){
		let token = userDefault.string(forKey: "authToken") as! String
		let deviceid = userDefault.string(forKey: "deviceIdentifier")
		let tracksRatio = userDefault.integer(forKey: "getTracksRatio")
		
		
		let json: Parameters = ["fields":["chapter":"", "deviceid": deviceid, "mediatype":"track", "ratio":tracksRatio], "method":"nexttrackbyratio"]

		let headers: HTTPHeaders = [
			"Content-Type":"application/json",
			"Authorization":"Bearer \(token)"
		]
		
		print(json)
		
		Alamofire.request(apiUrl!, method: .post, parameters: json, encoding: JSONEncoding.default, headers: headers).responseJSON {response in
			if let statusCode = response.response?.statusCode{
				if statusCode == 200{
					let jsonResult = response.result.value as! NSDictionary
					var trackInfo = jsonResult["result"] as! [[String:Any]]
					if trackInfo[0]["artist"] is NSNull{
						trackInfo[0]["artist"] = "Artist"
					}
					if trackInfo[0]["recname"] is NSNull{
						trackInfo[0]["recname"] = "Artist"
					}
					DispatchQueue.main.async {
						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"(\(requestCount+1))Получ. инфа о загруж. треке (\(trackInfo[0]["recid"]))"])
						print("Получена информация о следующем треке \(trackInfo[0]["recid"] ?? "")")
					}
					
					
					return complition(trackInfo[0])
				}else if statusCode == 401{
					self.GetAuthToken(){_ in
					}
					self.GetTrackInfo(requestCount: requestCount){_ in }
					NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetTrackNotAuthorized"])
					return complition(["NotAuthorized":statusCode.description])
				}else if statusCode == 500{
					return complition(["ServerError":statusCode.description])
//					self.RegisterDevice(){result in
//						print(self.userDefault.string(forKey: "deviceIdentifier"))
//						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetTrack deviceNotRegistered"])
//						if !result{
//							NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetTrack deviceStillNotRegistered"])
//							return complition(["NotAuthorized":statusCode.description])
//						}else{
//							self.GetTrackInfo(requestCount: requestCount){getTrackResponse in
//								NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetTrack device was not registered"])
//								return complition(getTrackResponse)
//							}
//						}
//					}
				}
			}else{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetTrackInfo FAIL:NotHttp"])
			}
		}
	}
	
	//Получение инфы об устройстве(userid)
	func GetDeviceInfo(completion: @escaping ([String:String])-> Void){
		let token = userDefault.string(forKey: "authToken") as! String
		let deviceid = userDefault.string(forKey: "deviceIdentifier")
		let json: [String:Any] = ["fields":["recid":deviceid], "method":"showdeviceinfo"]

		let headers: HTTPHeaders = [
			"Content-Type":"application/json",
			"Authorization":"Bearer \(token)"
		]
		
		Alamofire.request(apiUrl!, method: .post, parameters: json, encoding: JSONEncoding.default, headers: headers).responseJSON { response in
			if let statusCode = response.response?.statusCode{
				if statusCode == 200{
					let jsonResult = response.result.value as! NSDictionary
					let result = jsonResult["result"] as! NSDictionary
					if result["recid"] != nil{
						let returnValue: [String:String] = ["recid":result["recid"] as! String,
															"recname":result["recname"] as! String,
															"reccreated":result["reccreated"] as! String,
															"reccreatedby":result["reccreatedby"] as! String,
															"recstate":String(describing: result["recstate"]),
															"userid":result["userid"] as! String,
															"userid___value":result["userid___value"] as! String]
						return completion(returnValue)
					}
					else{
						self.RegisterDevice(){regResult in
							if regResult{
								self.GetDeviceInfo(){deviceInfo in
									return completion(deviceInfo)
								}
							}
						}
					}
					
				}else if statusCode == 401{
					NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"GetInfo fail: \(statusCode.description)"])
					self.GetAuthToken(){_ in
					}
					self.GetDeviceInfo(){res in
						completion(res)
					}
				}
			}
		}
	}
	//Сохранение истории
	func SaveHistory(historyId: String, trackId: String, isListen:Int){
		let deviceid = userDefault.string(forKey: "deviceIdentifier")
		let token = userDefault.string(forKey: "authToken") as! String
		
		let headers: HTTPHeaders = [
			"Content-Type":"application/json",
			"Authorization":"Bearer \(token)"
		]
		
		self.GetDeviceInfo(){deviceInfo in
			
			let userid = deviceInfo["userid"]
			let nowDate = NSDate()
			let dateFormatter = DateFormatter()
			dateFormatter.dateFormat = "yyyy-MM-dd H:m:s"
			dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
			let lastListen = dateFormatter.string(from: nowDate as Date)
			let json: Parameters = ["fields":["trackid":trackId.description,
														   "deviceid":(deviceid!.description),
											   "islisten":isListen.description,
											   "lastlisten":lastListen.description,
											   "userid":userid!.description,
											   "recid":historyId.description], "method":"savehistory"]
			Alamofire.request(self.apiUrl!, method: .post, parameters: json, encoding: JSONEncoding.default, headers: headers).responseJSON { response in
				if let statusCode = response.response?.statusCode{
					if statusCode == 401{
						self.GetAuthToken(){_ in
						}
						self.SaveHistory(historyId: historyId, trackId: trackId, isListen: isListen)
						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"SaveHistory notAuth: \(statusCode.description)"])
					}
					else if statusCode == 200{
						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"История сохранена"])
						CoreDataManager.instance.deleteHistoryFor(trackID: trackId)
					}
					else{
						NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Ист. не сохранена: \(statusCode.description)"])
					}
				}
			}
		}
	}
	
	//Установить корректность загруженного трека(битый или нет)
	func SetIsCorrect(trackId: String, isCorrect: Bool){
		let requestUrl = "http://rdev.ownradio.ru/odata/tracks(\(trackId))"
		let token = userDefault.string(forKey: "authToken") as! String
		
		let headers: HTTPHeaders = [
			"Content-Type":"application/json",
			"Authorization":"Bearer \(token)"
		]
		let json: Parameters = ["iscorrect":isCorrect]
		
		Alamofire.request(URL(string: requestUrl)!, method: .patch, parameters: json, encoding: JSONEncoding.default, headers: headers).response { response in
			let statusCode = response.response?.statusCode
			
			if statusCode == 401{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"SetIsCorrect unauth"])
				self.GetAuthToken(){_ in
					
				}
				self.SetIsCorrect(trackId: trackId, isCorrect: isCorrect)
			}
			else if statusCode == 400{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"SetIsCorrect track record not found"])
			}
			else if statusCode == 200{
				NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Инфа о поврежденном файле сохранена"])
			}
		}

		
	}
}
