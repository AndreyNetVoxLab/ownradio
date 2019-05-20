//
//  Api Service.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 11/22/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//
// Managing with api requests

import Foundation
import UIKit

class ApiService {
	
	let tracksUrl = URL(string: "http://api.ownradio.ru/v4/tracks/")
	var countRequest:Int! = 0
	static let shared = ApiService()
	init() {

	}

	//возвращает информацию о следующем треке
	func getTrackIDFromServer (requestCount: Int, complition:  @escaping ([String:AnyObject]) -> Void)  {
		print("call getTrackIDFromServer")
		//формируем URL для получения информации о следующем треке (trackId, name, artist, methodId, length)
		let trackurl = self.tracksUrl?.appendingPathComponent((UIDevice.current.identifierForVendor?.uuidString.lowercased())!).appendingPathComponent("/next")
		
		guard let url = trackurl else {
			NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Error: cannot create URL"])
			print("Error: cannot create URL")
			return
		}
		let urlRequest = NSURLRequest(url: url as URL)
		
		// set up the session
		let config = URLSessionConfiguration.default
		let session = URLSession(configuration: config)
		print(urlRequest.description)
		
		let task = session.dataTask(with: urlRequest as URLRequest, completionHandler: { (data, response, error) in
			// do stuff with response, data & error here
			
			if error != nil {
				if self.countRequest < 10 {
					Downloader.sharedInstance.runLoad(isSelf: false, complition: {
						
					})
				}
			}
			guard let data = data else {
				return
			}
			if let httpResponse = response as? HTTPURLResponse {
				if httpResponse.statusCode == 200 {
					
					do {
						let anyJson = try JSONSerialization.jsonObject(with: data, options: [])
						
						if let json = anyJson as? [String:AnyObject] {
							complition(json)
							NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"(\(requestCount+1))Получ. инфа о загруж. треке (\(json["id"]))"])
							print("Получена информация о следующем треке")
						}
						
					} catch (let error) {
						print("Achtung! Eror! \(error)")
					}
				}
			}
		})
		
		task.resume()
		
	}
	
	//функция сохранения истории прослушивания треков
    func saveHistory(historyId: String, trackId: String, isListen:Int) {
		
		let historyUrl = URL(string: "http://api.ownradio.ru/v5/histories/")
		//формируем URL для отправки истории прослушивания на сервер
		let trackHistoryUrl = historyUrl?.appendingPathComponent((UIDevice.current.identifierForVendor?.uuidString.lowercased())!).appendingPathComponent(trackId)
		
		guard let url = trackHistoryUrl else {
			NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Error: cannot create URL"])
			print("Error: cannot create URL")
			return
		}
		
		let request = NSMutableURLRequest(url: url as URL)
		request.httpMethod = "POST"
		let nowDate = NSDate()
		let dateFormatter = DateFormatter()
		dateFormatter.dateFormat = "yyyy-MM-dd'T'H:m:s"
		dateFormatter.timeZone = TimeZone(secondsFromGMT: 0)
		let lastListen = dateFormatter.string(from: nowDate as Date)

		request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
		
		//добавляем параметры в тело запроса: lastListen - дата и время последнего прослушивания трека,
		//isListen - флаг прослушан или пропущен трек, methodid - метод выдачи трека пользователю
		let dict = ["recid":historyId, "lastListen":lastListen, "isListen":isListen] as [String : Any]
		do {
			let data = try JSONSerialization.data(withJSONObject: dict, options: [])
//			let dataString = String(data: data, encoding: String.Encoding.utf8)!
			request.httpBody = data
			
			
		} catch {
			NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"JSON serialization failed: \(error)"])
			print("JSON serialization failed:  \(error)")
		}
		
		//отправляем историю на сервер
		let task = URLSession.shared.dataTask(with: request as URLRequest){ data,response,error in
			
			if error != nil{
				print(error?.localizedDescription)
				return
			}
			
			if let httpResponse = response as? HTTPURLResponse {
				print("status code \(httpResponse.statusCode)")
                if (httpResponse.allHeaderFields["Location"] as? String) != nil || httpResponse.statusCode == 201 {
                        if data != nil {
                            //если история передана успешна - удаляем из таблицы история запись об этом треке
                            CoreDataManager.instance.deleteHistoryFor(trackID: trackId)
                            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Ист. перед., код: \(httpResponse.statusCode)"])
                            print("История передана на сервер")
						
                            //			let dataString = String(data: data!, encoding: String.Encoding.utf8)!
                        }
                } else {
                    NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Ист. не перед. код:\(httpResponse.statusCode)"])
                }
			}
			
		}
		task.resume()
		
	}
    
    //Функция регистрации устройства
    func registerDevice(){
        //формируем URL
        let registerDeviceUrl = URL(string: "http://api.ownradio.ru/v5/devices")
        guard let url = registerDeviceUrl else {
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Error: cannot create registerDeviceUrl"])
            print("Error: cannot create registerDeviceUrl")
            return
        }
        
        let request = NSMutableURLRequest(url: url as URL)
        request.httpMethod = "POST"
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        let systemVersion = UIDevice.current.systemVersion
        let model = UIDevice.current.model
        var deviceName: String? = model + " " + systemVersion
        
        if let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String {
            if (Bundle.main.infoDictionary?["CFBundleVersion"] as? String) != nil {
                deviceName = deviceName! + " " + "v" + version
            }
        }
        
        //добавляем параметры в тело запроса
        let dict = ["recid":(UIDevice.current.identifierForVendor?.uuidString.lowercased())!, "recname":deviceName ?? "New iOS device"] as [String : Any]
        do {
            let data = try JSONSerialization.data(withJSONObject: dict, options: [])
            request.httpBody = data
        } catch {
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"JSON serialization failed: \(error)"])
            print("JSON serialization failed:  \(error)")
        }
        
        let task = URLSession.shared.dataTask(with: request as URLRequest){ data,response,error in
            
            if error != nil{
                print(error?.localizedDescription)
                return
            }
            
            if let httpResponse = response as? HTTPURLResponse {
                print("status code \(httpResponse.statusCode)")
                if (httpResponse.allHeaderFields["Location"] as? String) != nil || httpResponse.statusCode == 201 {
                    if data != nil {
                        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Устройство зарегистрировано, код: \(httpResponse.statusCode)"])
                        print("Устройство зарегистрировано")
                    }
                } else {
                    NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Сбой регистрации устройства, код: \(httpResponse.statusCode)"])
                }
            }
            
        }
        task.resume()
        
    }
    
    //функция сохранения истории прослушивания треков
    func setTrackIsCorrect(trackId: String, isCorrect: Int) {
        
        let trackUrl = URL(string: "http://api.ownradio.ru/v5/tracks/")
        //формируем URL
        let setTrackIsCorrect = trackUrl?.appendingPathComponent(trackId).appendingPathComponent((UIDevice.current.identifierForVendor?.uuidString.lowercased())!)
        
        guard let url = setTrackIsCorrect else {
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Error: cannot create URL"])
            print("Error: cannot create URL")
            return
        }
        
        let request = NSMutableURLRequest(url: url as URL)
        request.httpMethod = "POST"
        request.setValue("application/json; charset=utf-8", forHTTPHeaderField: "Content-Type")
        
        //добавляем параметры в тело запроса: iscorrect - 0, если файл битый
        let dict = ["iscorrect":isCorrect] as [String : Any]
        do {
            let data = try JSONSerialization.data(withJSONObject: dict, options: [])
            request.httpBody = data
            
        } catch {
            NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"JSON serialization failed: \(error)"])
            print("JSON serialization failed:  \(error)")
        }
        
        let task = URLSession.shared.dataTask(with: request as URLRequest){ data,response,error in
            
            if error != nil{
                print(error?.localizedDescription)
                return
            }
            
            if let httpResponse = response as? HTTPURLResponse {
                print("status code \(httpResponse.statusCode)")
                if httpResponse.statusCode == 201 {
                    if data != nil {
                        NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Трек помечен некорректным, код: \(httpResponse.statusCode)"])
                        print("Трек помечен некорректным")
                    }
                } else {
                    NotificationCenter.default.post(name: NSNotification.Name(rawValue: "updateSysInfo"), object: nil, userInfo: ["message":"Сбой отправки информации о битом треке, код: \(httpResponse.statusCode)"])
                }
            }
            
        }
        task.resume()
        
    }

}
