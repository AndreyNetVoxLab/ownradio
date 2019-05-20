//
//  HistoryEntity+CoreDataProperties.swift
//  OwnRadio
//
//  Created by Roman Litoshko on 12/8/16.
//  Copyright © 2016 Roll'n'Code. All rights reserved.
//

import Foundation
import CoreData


extension HistoryEntity {

    @nonobjc public class func fetchRequest() -> NSFetchRequest<HistoryEntity> {
        return NSFetchRequest<HistoryEntity>(entityName: "HistoryEntity");
    }

    @NSManaged public var isListen: Int32
    @NSManaged public var recCreated: String?
    @NSManaged public var recId: String?
    @NSManaged public var trackId: String?

}
