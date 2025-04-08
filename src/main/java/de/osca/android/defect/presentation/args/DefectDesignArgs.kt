package de.osca.android.defect.presentation.args

import de.osca.android.essentials.presentation.component.design.ModuleDesignArgs

/**
 * This is the Defect Interface for Defect
 * It contains design arguments explicit for this module but also
 * generic module arguments which are by default null and can be set to override
 * the masterDesignArgs
 *
 * no properties for this module design
 */
interface DefectDesignArgs : ModuleDesignArgs {
    val mapStyle: Int?
}