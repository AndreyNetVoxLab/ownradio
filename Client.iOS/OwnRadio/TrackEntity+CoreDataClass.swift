//
//  TrackEntity+CoreDataClass.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 12/6/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//

import Foundation
import CoreData


public class TrackEntity: NSManagedObject {
	convenience init() {
		self.init(entity: CoreDataManager.instance.entityForName(entityName: "TrackEntity"), insertInto: CoreDataManager.instance.managedObjectContext)
	}
}
